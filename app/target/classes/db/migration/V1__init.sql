-- Baseline schema for NFC backend (condensed)

CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(320) UNIQUE NOT NULL,
  phone VARCHAR(50),
  password_hash VARCHAR(255),
  status VARCHAR(50) DEFAULT 'UNVERIFIED',
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS profiles (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  username VARCHAR(100) UNIQUE,
  display_name VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS nfc_cards (
  id BIGSERIAL PRIMARY KEY,
  internal_code VARCHAR(100) UNIQUE NOT NULL,
  chip_uid VARCHAR(255),
  status VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS nfc_assignments (
  id BIGSERIAL PRIMARY KEY,
  nfc_card_id BIGINT REFERENCES nfc_cards(id),
  profile_id BIGINT REFERENCES profiles(id),
  assigned_at TIMESTAMPTZ DEFAULT now(),
  unassigned_at TIMESTAMPTZ
);

-- user_sessions for refresh token listing
CREATE TABLE IF NOT EXISTS user_sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash VARCHAR(255) NOT NULL UNIQUE,
  device_name VARCHAR(255),
  ip_address INET,
  created_at TIMESTAMPTZ DEFAULT now(),
  last_used_at TIMESTAMPTZ DEFAULT now(),
  expires_at TIMESTAMPTZ NOT NULL,
  revoked BOOLEAN NOT NULL DEFAULT FALSE
);
