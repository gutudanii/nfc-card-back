# cards.toollix.app — Backend Development Documentation
**Version 1.0 · Modular Monolith · Java 21 + Spring Boot 3.x**

---

## 1. System Overview

Single Spring Boot application, modular by domain, backed by PostgreSQL. Public profile reads are cached/fast; NFC tap and analytics writes are async. Every permission check resolves through **Profile → (User | Organization)**, never hardcoded per feature.

```
Client → Nginx → Spring Boot Monolith → PostgreSQL / Redis / Object Storage
```

---

## 2. Tech Stack

| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Security | Spring Security + JWT (access + refresh) |
| ORM | Spring Data JPA / Hibernate |
| DB | PostgreSQL 16 |
| Cache/Queue | Redis |
| Migrations | Flyway |
| Storage | S3-compatible (Cloudflare R2 / DO Spaces) |
| Docs | springdoc-openapi (Swagger UI) |
| Build | Maven or Gradle |

---

## 3. Module Structure

```
auth · users · profiles · organizations · teams · nfc · templates
analytics · orders · payments · subscriptions · notifications · admin · common
```

**Rule:** modules call each other only via `@Service` interfaces. No cross-module repository access. This is what allows extracting `nfc` or `analytics` into a separate service later without a rewrite.

---

## 4. Authentication

### 4.1 Flow
```
POST /auth/register      → email/phone + password → creates User (unverified)
POST /auth/verify        → OTP/email link → activates account
POST /auth/login         → returns access_token (15 min) + refresh_token (30 days, httpOnly cookie)
POST /auth/refresh       → rotates access_token
POST /auth/logout        → revokes refresh_token (stored in Redis/DB denylist)
POST /auth/oauth/google  → Phase 2
POST /auth/oauth/apple   → Phase 2
```

- Passwords: bcrypt (cost 12+).
- Access token: JWT, short-lived, carries `user_id` + `token_version` only — **no roles baked in** (roles change too often; baking them in causes stale-permission bugs).
- Refresh token: opaque, stored server-side (Redis), rotated on every use, revocable (device logout, password change, suspicious activity).
- Rate limit `/auth/login` and `/auth/register` per IP + per identifier (Redis sliding window) to block brute force.

### 4.2 Account Types (top-level)

| Type | Description |
|---|---|
| `INDIVIDUAL_USER` | Default. One personal profile. |
| `PROFESSIONAL_USER` | Same `User` entity, `profile.mode = PROFESSIONAL`. Not a separate account type — an entitlement + profile flag. |
| `ORGANIZATION_OWNER` | A `User` who created an `Organization`. Still a normal user account — org ownership is a relationship, not an account type. |

**Key decision:** there is only ever one `users` table. "Individual" vs "company" is not an account-type fork — it's a relationship (`organization_members`) layered on top of a normal user. This avoids duplicating auth logic for two different account systems.

---

## 5. Authorization Model

### 5.1 Two independent role dimensions

**Platform roles** (rare, internal staff only):
```
SUPER_ADMIN · SUPPORT_ADMIN · OPERATIONS_ADMIN
```

**Context roles** (resolved per-resource, not on the JWT):
```
ORGANIZATION_OWNER · ORGANIZATION_ADMIN · TEAM_MANAGER · MEMBER
```

A request to modify resource `X` is authorized like this:

```
1. Resolve X → profile_id
2. Resolve profile_id → owner_user_id  OR  organization_id
3. If owner_user_id == requester.id → ALLOW (self-owned)
4. Else if organization_id present →
      look up requester's OrganizationMember row for that org
      check role against required permission for the action
5. Else → DENY
```

Implement this as a single `AccessGuard` service (`common` module), called from a method-security annotation (`@PreAuthorize("@accessGuard.canEdit(#profileId, principal)")`) — never duplicated per controller.

### 5.2 Permission Matrix

| Action | Individual (self) | Org Member | Team Manager | Org Admin | Org Owner | Platform Admin |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| Edit own profile | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Edit another member's profile | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| View org analytics (own) | — | ✅ | ✅ | ✅ | ✅ | ✅ |
| View org analytics (all members) | — | ❌ | ✅ (own team) | ✅ | ✅ | ✅ |
| Add/remove members | — | ❌ | ❌ | ✅ | ✅ | ✅ |
| Bulk import employees | — | ❌ | ❌ | ✅ | ✅ | ✅ |
| Assign/reassign NFC card | ✅ (own) | ❌ | ❌ | ✅ | ✅ | ✅ |
| Change org billing/plan | — | ❌ | ❌ | ❌ | ✅ | ✅ |
| Suspend a user/card | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| Manage NFC inventory (global) | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |

### 5.3 Multi-org membership

`organization_members` is a many-to-many join with its own `role` and `department` columns — a user can be a solo `INDIVIDUAL_USER` on their personal card **and** a `MEMBER` of a client's organization simultaneously. No conflict, because authorization is always resolved per-resource, never globally cached on the session.

---

## 6. Individual vs Company: Card & NFC Ownership Model

This is the core business rule the whole system hangs on.

```
Individual path:
  User (1) ── owns ──> Profile (1) ── has ──> DigitalCard (1) ── may have ──> NfcAssignment ──> NfcCard

Company path:
  Organization (1) ── has many ──> OrganizationMember ──> User ──> Profile ──> DigitalCard ──> NfcAssignment ──> NfcCard
  Organization (1) ── has ──> CompanyProfile (its own public directory page)
```

**Rules:**
1. Every `NfcCard` points at exactly one *current* `NfcAssignment` → `profile_id`. It never points directly at a `User` or `Organization` — always at the profile, because the profile is the thing with a stable public URL.
2. When an employee leaves an organization: the `OrganizationMember` row is deactivated, but the `Profile` and its `NfcAssignment` history stay intact for audit purposes. The **organization admin** decides whether to reassign that employee's physical card to a new hire (creates a new `NfcAssignment` row; old one gets `unassigned_at` timestamp — never deleted).
3. Individual users manage their own NFC lifecycle (report lost, request replacement) through self-service endpoints. Organization members' NFC lifecycle is managed by `ORGANIZATION_ADMIN` or `TEAM_MANAGER` only — an employee cannot self-reassign a company-owned card.
4. Ownership of the *physical* card (who paid for it) is tracked separately on `orders` (`user_id` or `org_id`), decoupled from *assignment* (who it currently points to). This matters for billing: a company can buy 50 cards (`order.org_id`) that get assigned to individual employee profiles over time.

### 6.1 NFC Lifecycle State Machine

```
UNASSIGNED → PROGRAMMED → ASSIGNED → ACTIVE → (LOST | DAMAGED) → RETIRED
                                          ↑___________________________|
                                          (replacement creates new ACTIVE card,
                                           same profile)
```

| State | Who can trigger | Trigger endpoint |
|---|---|---|
| `PROGRAMMED` | Platform admin / ops | `POST /admin/nfc/{id}/program` |
| `ASSIGNED` | Self (individual) or Org Admin | `POST /nfc/{id}/assign` |
| `ACTIVE` | System (auto, after first successful tap) | — |
| `LOST` | Self or Org Admin | `POST /nfc/{id}/report-lost` |
| `RETIRED` | Platform admin | `POST /admin/nfc/{id}/retire` |

---

## 7. Core Entities (condensed schema)

```sql
users(id, email, phone, password_hash, status, created_at)

profiles(id, user_id FK UNIQUE, username UNIQUE, mode ENUM[PERSONAL,PROFESSIONAL],
         display_name, job_title, bio, photo_url, is_public, org_id FK NULLABLE)

digital_cards(id, profile_id FK UNIQUE, template_id FK, published_at)

-- ⚠️ display_order is required: the dashboard /links page supports drag-and-drop
-- reordering. Without this column the order cannot be persisted server-side.
social_links / contact_links(id, profile_id FK, type, value,
                              visibility ENUM[PUBLIC, PRIVATE],
                              display_order INTEGER NOT NULL DEFAULT 0)

services(id, profile_id FK, title, description)

-- ⚠️ Extended schema required: the dashboard /portfolio page supports three
-- content types (PROJECT, SERVICE, EXPERIENCE) and per-item metadata including
-- category labels, external URLs, price text, and cover images.
portfolio_items(
  id, profile_id FK,
  item_type    ENUM[PROJECT, SERVICE, EXPERIENCE] NOT NULL,
  title        TEXT NOT NULL,
  description  TEXT,
  category     VARCHAR(100),     -- e.g. "Design", "Development"
  external_url TEXT,             -- optional project link
  image_url    TEXT,             -- cover photo stored in object storage
  price_text   VARCHAR(50),      -- e.g. "From $500" (services only)
  display_order INTEGER NOT NULL DEFAULT 0,
  created_at   TIMESTAMPTZ DEFAULT NOW()
)

-- ⚠️ Branding columns required: the /org/[orgId]/branding page lets owners
-- set a company logo, brand color, and tagline. None of these existed before.
organizations(
  id, name, slug UNIQUE, plan_id FK,
  logo_url     TEXT,             -- uploaded to object storage
  brand_color  VARCHAR(7),       -- hex, e.g. "#10b981"
  tagline      TEXT
)
organization_members(id, org_id FK, user_id FK, role, department, status, joined_at)
teams(id, org_id FK, name)

nfc_cards(id, internal_code UNIQUE, chip_uid NULLABLE, status, manufactured_at)
nfc_assignments(id, nfc_card_id FK, profile_id FK, assigned_at, unassigned_at NULLABLE)
  -- append-only history, never mutated in place

analytics_events(id, profile_id FK, nfc_card_id FK NULLABLE, event_type,
                  occurred_at, meta JSONB)

plans(id, code, name, price, billing_cycle, features JSONB)
subscriptions(id, subject_type ENUM[USER,ORGANIZATION], subject_id, plan_id FK,
              status, expires_at)

orders(id, user_id FK NULLABLE, org_id FK NULLABLE, product_type, quantity,
       amount, status)
payments(id, order_id FK NULLABLE, subscription_id FK NULLABLE, provider,
         txn_ref, amount, status)

-- ⚠️ Required for /dashboard/settings Active Sessions panel.
-- Stores one row per issued refresh token so devices can be listed + revoked.
user_sessions(
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash   VARCHAR(255) NOT NULL UNIQUE, -- bcrypt/SHA-256 of opaque refresh token
  device_name  VARCHAR(255),  -- parsed from User-Agent, e.g. "iPhone 15 Pro"
  ip_address   INET,
  location     VARCHAR(255),  -- GeoIP resolved city + country
  created_at   TIMESTAMPTZ DEFAULT NOW(),
  last_used_at TIMESTAMPTZ DEFAULT NOW(),
  expires_at   TIMESTAMPTZ NOT NULL,
  revoked      BOOLEAN NOT NULL DEFAULT FALSE
)
```

**Note:** `subscriptions.subject_type/subject_id` is polymorphic — one billing engine serves individual, professional, and org plans.

---

## 8. Entitlement Service (feature gating)

Single source of truth for "can this account do X" — never scattered `if (plan == "PRO")` checks in controllers.

```java
entitlementService.require(profileId, Feature.NFC_CARD);
entitlementService.require(profileId, Feature.ADVANCED_ANALYTICS);
entitlementService.require(orgId, Feature.BULK_IMPORT);
```

Resolves: `profile → user/org → active subscription → plan.features JSONB → boolean`.

---

## 9. Key API Endpoints (by module)

| Module | Endpoint | Notes |
|---|---|---|
| auth | `POST /auth/register`, `/login`, `/refresh`, `/logout` | see §4.1 |
| **auth** | **`GET /auth/sessions`** | **Returns all active sessions for the current user (device name, IP, location, last active). Powers the /settings Active Sessions UI.** |
| **auth** | **`DELETE /auth/sessions/{sessionId}`** | **Revokes a specific refresh token by ID. Powers the "Revoke" button per device.** |
| profiles | `GET /p/{username}` (public) | cached, ISR-friendly, no auth |
| profiles | `PUT /me/profile` | self-edit, entitlement-gated fields |
| **profiles** | **`PUT /me/profile/links/reorder`** | **Accepts an ordered array of link IDs and updates `display_order` on each row. Powers the drag-and-drop link list on /dashboard/links.** |
| **profiles** | **`PATCH /me/profile/template`** | **Updates `digital_cards.template_id` for the current user. Powers the /dashboard/template picker page.** |
| **profiles** | **`POST /me/profile/portfolio`** | **Creates a new portfolio item (PROJECT, SERVICE, or EXPERIENCE) with cover image upload to object storage. Powers /dashboard/portfolio add modal.** |
| **profiles** | **`PATCH /me/profile/portfolio/{itemId}`** | **Updates an existing portfolio item.** |
| **profiles** | **`DELETE /me/profile/portfolio/{itemId}`** | **Deletes a portfolio item.** |
| nfc | `GET /tap/{internal_code}` | public, redirect + async analytics write |
| nfc | `POST /nfc/{id}/assign` , `/report-lost` | self or org-admin gated |
| organizations | `POST /orgs`, `POST /orgs/{id}/members/bulk-import` | admin only |
| **organizations** | **`PATCH /orgs/{id}/branding`** | **Accepts `logo` (multipart), `brand_color`, `tagline`. Saves logo to object storage, updates org row. Powers /org/[orgId]/branding live-preview page.** |
| **organizations** | **`POST /orgs/{id}/members/validate-csv`** | **Pre-flight endpoint: accepts the uploaded CSV/XLSX, parses + validates rows, returns a preview payload (valid rows + error rows) WITHOUT inserting anything. Powers the import preview table shown before final confirmation.** |
| analytics | `GET /me/analytics`, `GET /orgs/{id}/analytics` | entitlement-gated depth |
| orders | `POST /orders`, `GET /orders/{id}` | |
| subscriptions | `POST /subscriptions`, `GET /me/subscription` | |
| admin | `GET /admin/nfc/inventory`, `/admin/orders` | platform roles only |

Full OpenAPI spec to be generated via springdoc once controllers are scaffolded.

---

## 10. Security Checklist

- HTTPS everywhere (Nginx + Let's Encrypt / Cloudflare)
- JWT access token short-lived; refresh token rotated + revocable
- bcrypt password hashing
- Rate limiting on auth, `/tap/`, and public profile endpoints (Redis)
- Input validation on every DTO (Bean Validation)
- File upload: type/size validation, virus scan optional, store in object storage never on app server
- Audit log table for admin actions (suspend user, reassign NFC, change plan)
- DB backups automated, stored off-VPS
- Privacy: `visibility` flag enforced at the query layer, not just hidden in UI

---

## 11. Build Order

1. auth + users + profiles + digital_cards + public profile read API
2. nfc module: inventory, assignment, `/tap/` redirect, analytics event capture
3. subscriptions + entitlement service + payments abstraction (Telebirr/manual first)
4. orders (NFC purchase → delivery pipeline)
5. organizations: members, teams, bulk import, company directory
6. admin dashboard endpoints

---

*Frontend documentation (Next.js) to follow as a companion document once this is confirmed.*
