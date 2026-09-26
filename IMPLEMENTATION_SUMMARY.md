# NFC Backend Implementation Summary

## Architecture

The backend is organized as a modular monolith built with Java 21 and Spring Boot 3.x. Modules are separated by domain responsibility and run inside one application process:

- common: shared security, JWT, audit, exception handling, mail abstraction
- users: persisted user account model
- auth: registration, verification, login, refresh, session management
- profiles: personal and professional profile data
- organizations: orgs, members, teams, branding metadata
- nfc: card lifecycle and assignment logic
- analytics: event recording and async processing
- orders: order creation and retrieval
- subscriptions: plan and subscription data, billing subject semantics
- admin: operational inventory and admin dashboards
- app: Spring Boot bootstrap, Flyway, datasource, runtime config

Core rules:

- Authentication is JWT-based with short-lived access tokens and rotated refresh tokens.
- Passwords are BCrypt-hashed before persistence.
- DB migrations are managed with Flyway.
- Audit actions are stored centrally through a shared `AuditService`.
- Business rules are kept around profiles, organizations, cards, and subscriptions rather than hardcoded in controllers.
- The app supports async event handling for analytics writes.

## Current Implementation State

Implemented:

- Spring Boot multi-module Maven project
- JWT auth and refresh flow
- BCrypt password hashing
- user persistence + verification token flow
- basic profile CRUD
- NFC card create / assign / lost / retire flows
- org, member, and team scaffolding
- orders, subscriptions, and admin routes
- file-based and SMTP-switchable mail abstraction
- global exception handling
- Flyway migration schema for core tables

Still intentionally scaffolded for production adaptation:

- entitlements are wired to feature constants and require DB-backed plan checks
- email provider is SMTP-ready but default no-op for safe local development
- rate limiting is not yet Redis-backed
- production deployment manifests and secret management remain to be finalized

## API Overview

### Auth
- `POST /auth/register`
- `POST /auth/verify`
- `POST /auth/login`
- `POST /auth/refresh`
- `GET /auth/sessions`
- `DELETE /auth/sessions/{sessionId}`

### Profile
- `GET /p/{username}`
- `PUT /me/profile`

### Organization
- `POST /orgs`
- `GET /orgs/{slug}`
- `POST /orgs/{orgId}/members`
- `GET /orgs/{orgId}/members`
- `POST /orgs/{orgId}/teams`

### NFC
- `GET /tap/{internalCode}`
- `POST /nfc/{id}/assign`
- `POST /nfc/{id}/report-lost`
- `POST /admin/nfc/{id}/retire`

### Orders
- `POST /orders`
- `GET /orders/{id}`

### Subscriptions
- `POST /subscriptions`

### Admin
- `GET /admin/nfc/inventory`
- `GET /admin/orders`

### System
- `GET /actuator/health`
