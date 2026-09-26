-- V10: Dynamic Plans Catalog, NFC Card Types, and Org-based subscriptions
-- Toollix Pricing (in Ethiopian Birr stored as cents, i.e. ×100)

-- ─────────────────────────────────────────────────────────────────────
-- 1. Enhance plans table with richer metadata
-- ─────────────────────────────────────────────────────────────────────
ALTER TABLE plans ADD COLUMN IF NOT EXISTS target_type  VARCHAR(50)  NOT NULL DEFAULT 'INDIVIDUAL'; -- INDIVIDUAL | TEAM
ALTER TABLE plans ADD COLUMN IF NOT EXISTS billing_type VARCHAR(50)  NOT NULL DEFAULT 'LIFETIME';   -- LIFETIME | MONTHLY
ALTER TABLE plans ADD COLUMN IF NOT EXISTS min_seats    INT          NOT NULL DEFAULT 1;
ALTER TABLE plans ADD COLUMN IF NOT EXISTS max_seats    INT          NOT NULL DEFAULT 1;
ALTER TABLE plans ADD COLUMN IF NOT EXISTS min_months   INT          NOT NULL DEFAULT 1;          -- min commitment for MONTHLY
ALTER TABLE plans ADD COLUMN IF NOT EXISTS active       BOOLEAN      NOT NULL DEFAULT TRUE;
ALTER TABLE plans ADD COLUMN IF NOT EXISTS sort_order   INT          NOT NULL DEFAULT 0;
ALTER TABLE plans ADD COLUMN IF NOT EXISTS description  TEXT;

-- ─────────────────────────────────────────────────────────────────────
-- 2. NFC card types table
-- ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS nfc_card_types (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50)  UNIQUE NOT NULL,  -- 'white' | 'black' | 'gold'
    name        VARCHAR(100) NOT NULL,
    price_cents BIGINT       NOT NULL,         -- price in ETB×100
    description TEXT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Add card_type to nfc_cards
ALTER TABLE nfc_cards ADD COLUMN IF NOT EXISTS card_type_id BIGINT REFERENCES nfc_card_types(id);

-- ─────────────────────────────────────────────────────────────────────
-- 3. Seed: NFC Card Types
-- ─────────────────────────────────────────────────────────────────────
INSERT INTO nfc_card_types (code, name, price_cents, description) VALUES
  ('white', 'Classic White', 100000, 'Standard white NFC card — clean, minimalist finish'),
  ('black', 'Stealth Black', 120000, 'Premium black NFC card — bold, professional look'),
  ('gold',  'Gold Elite',   150000, 'Gold luxury NFC card — premium metallic finish')
ON CONFLICT (code) DO UPDATE SET
  name        = EXCLUDED.name,
  price_cents = EXCLUDED.price_cents,
  description = EXCLUDED.description;

-- ─────────────────────────────────────────────────────────────────────
-- 4. Seed: Individual Plans
-- ─────────────────────────────────────────────────────────────────────
-- Individual: NFC Card Only (no profile plan, just the physical card handled via nfc_card_types)
-- Individual: Monthly (profile plan, 200 ETB/month, min 3 months)
-- Individual: Lifetime (NFC + lifetime profile, 3,500 ETB)

INSERT INTO plans (code, name, description, billing_cycle, billing_type, target_type, min_seats, max_seats, min_months, price_cents, sort_order, active, features) VALUES
  (
    'ind_monthly',
    'Individual Monthly',
    'Full digital profile access billed monthly. Minimum 3-month commitment.',
    'MONTHLY', 'MONTHLY', 'INDIVIDUAL', 1, 1, 3,
    20000,   -- 200 ETB/month
    10, TRUE,
    '{"items": ["Digital profile page", "All contact links", "Premium templates", "Analytics dashboard", "QR code sharing", "vCard download"]}'::jsonb
  ),
  (
    'ind_lifetime',
    'Individual Lifetime',
    'One-time payment for lifetime profile access. Includes your NFC card.',
    'LIFETIME', 'LIFETIME', 'INDIVIDUAL', 1, 1, 1,
    350000,  -- 3,500 ETB
    20, TRUE,
    '{"items": ["Everything in Monthly", "Lifetime access — no renewal", "NFC card included", "Priority support", "All future templates"]}'::jsonb
  )
ON CONFLICT (code) DO UPDATE SET
  name        = EXCLUDED.name,
  description = EXCLUDED.description,
  price_cents = EXCLUDED.price_cents,
  features    = EXCLUDED.features,
  active      = EXCLUDED.active;

-- ─────────────────────────────────────────────────────────────────────
-- 5. Seed: Team Plans (5–20 members)
-- ─────────────────────────────────────────────────────────────────────
INSERT INTO plans (code, name, description, billing_cycle, billing_type, target_type, min_seats, max_seats, min_months, price_cents, sort_order, active, features) VALUES
  (
    'team_sm_monthly',
    'Small Team Monthly (5–20)',
    '150 ETB/member/month for teams of 5 to 20. Minimum 3-month commitment.',
    'MONTHLY', 'MONTHLY', 'TEAM', 5, 20, 3,
    15000,   -- 150 ETB/member/month
    30, TRUE,
    '{"items": ["Organization dashboard", "Member management", "Branded NFC cards", "Shared analytics", "Bulk profile management", "Custom branding"]}'::jsonb
  ),
  (
    'team_sm_lifetime',
    'Small Team Lifetime (5–20)',
    '3,000 ETB/member one-time for teams of 5 to 20.',
    'LIFETIME', 'LIFETIME', 'TEAM', 5, 20, 1,
    300000,  -- 3,000 ETB/member
    40, TRUE,
    '{"items": ["Everything in Monthly", "Lifetime access for whole team", "NFC cards included", "Priority onboarding", "All future templates"]}'::jsonb
  )
ON CONFLICT (code) DO UPDATE SET
  name        = EXCLUDED.name,
  description = EXCLUDED.description,
  price_cents = EXCLUDED.price_cents,
  features    = EXCLUDED.features;

-- ─────────────────────────────────────────────────────────────────────
-- 6. Seed: Team Plans (21–50 members)
-- ─────────────────────────────────────────────────────────────────────
INSERT INTO plans (code, name, description, billing_cycle, billing_type, target_type, min_seats, max_seats, min_months, price_cents, sort_order, active, features) VALUES
  (
    'team_md_monthly',
    'Medium Team Monthly (21–50)',
    '100 ETB/member/month for teams of 21 to 50. Minimum 3-month commitment.',
    'MONTHLY', 'MONTHLY', 'TEAM', 21, 50, 3,
    10000,   -- 100 ETB/member/month
    50, TRUE,
    '{"items": ["Organization dashboard", "Member management", "Branded NFC cards", "Shared analytics", "Department grouping", "API access"]}'::jsonb
  ),
  (
    'team_md_lifetime',
    'Medium Team Lifetime (21–50)',
    '2,600 ETB/member one-time for teams of 21 to 50.',
    'LIFETIME', 'LIFETIME', 'TEAM', 21, 50, 1,
    260000,  -- 2,600 ETB/member
    60, TRUE,
    '{"items": ["Everything in Monthly", "Lifetime access for whole team", "NFC cards included", "Dedicated account manager"]}'::jsonb
  )
ON CONFLICT (code) DO UPDATE SET
  name        = EXCLUDED.name,
  description = EXCLUDED.description,
  price_cents = EXCLUDED.price_cents,
  features    = EXCLUDED.features;

-- ─────────────────────────────────────────────────────────────────────
-- 7. Seed: Team Plans (51–100 members)
-- ─────────────────────────────────────────────────────────────────────
INSERT INTO plans (code, name, description, billing_cycle, billing_type, target_type, min_seats, max_seats, min_months, price_cents, sort_order, active, features) VALUES
  (
    'team_lg_monthly',
    'Large Team Monthly (51–100)',
    '100 ETB/member/month for teams of 51 to 100. Minimum 3-month commitment.',
    'MONTHLY', 'MONTHLY', 'TEAM', 51, 100, 3,
    10000,
    70, TRUE,
    '{"items": ["Organization dashboard", "Member management", "Branded NFC cards", "Advanced analytics", "API access", "SSO ready"]}'::jsonb
  ),
  (
    'team_lg_lifetime',
    'Large Team Lifetime (51–100)',
    '2,000 ETB/member one-time for teams of 51 to 100.',
    'LIFETIME', 'LIFETIME', 'TEAM', 51, 100, 1,
    200000,
    80, TRUE,
    '{"items": ["Everything in Monthly", "Lifetime access", "NFC cards included", "Custom onboarding"]}'::jsonb
  )
ON CONFLICT (code) DO UPDATE SET
  name        = EXCLUDED.name,
  description = EXCLUDED.description,
  price_cents = EXCLUDED.price_cents,
  features    = EXCLUDED.features;

-- ─────────────────────────────────────────────────────────────────────
-- 8. Seed: Enterprise Plans (100+ members)
-- ─────────────────────────────────────────────────────────────────────
INSERT INTO plans (code, name, description, billing_cycle, billing_type, target_type, min_seats, max_seats, min_months, price_cents, sort_order, active, features) VALUES
  (
    'enterprise_monthly',
    'Enterprise Monthly (100+)',
    '100 ETB/member/month for enterprises of 100+ members. Minimum 3-month commitment.',
    'MONTHLY', 'MONTHLY', 'TEAM', 101, 99999, 3,
    10000,
    90, TRUE,
    '{"items": ["Everything in Large Team", "Unlimited members", "Custom SLA", "White-label option", "Dedicated infrastructure"]}'::jsonb
  ),
  (
    'enterprise_lifetime',
    'Enterprise Lifetime (100+)',
    '1,500 ETB/member one-time for enterprises of 100+ members.',
    'LIFETIME', 'LIFETIME', 'TEAM', 101, 99999, 1,
    150000,
    100, TRUE,
    '{"items": ["Everything in Monthly", "Lifetime access", "NFC cards (500 ETB each)", "Custom onboarding", "Full API access"]}'::jsonb
  )
ON CONFLICT (code) DO UPDATE SET
  name        = EXCLUDED.name,
  description = EXCLUDED.description,
  price_cents = EXCLUDED.price_cents,
  features    = EXCLUDED.features;
