DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'profiles' AND column_name = 'mode'
    ) THEN
        ALTER TABLE profiles ADD COLUMN mode VARCHAR(50) NOT NULL DEFAULT 'PERSONAL';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'profiles' AND column_name = 'org_id'
    ) THEN
        ALTER TABLE profiles ADD COLUMN org_id BIGINT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'profiles' AND column_name = 'is_public'
    ) THEN
        ALTER TABLE profiles ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT TRUE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'profiles' AND column_name = 'created_at'
    ) THEN
        ALTER TABLE profiles ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'nfc_cards' AND column_name = 'manufactured_at'
    ) THEN
        ALTER TABLE nfc_cards ADD COLUMN manufactured_at TIMESTAMPTZ;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'organizations' AND column_name = 'logo_url'
    ) THEN
        ALTER TABLE organizations ADD COLUMN logo_url VARCHAR(255);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'organizations' AND column_name = 'brand_color'
    ) THEN
        ALTER TABLE organizations ADD COLUMN brand_color VARCHAR(50);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'organizations' AND column_name = 'tagline'
    ) THEN
        ALTER TABLE organizations ADD COLUMN tagline VARCHAR(255);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS analytics_events (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT,
    nfc_card_id BIGINT,
    event_type VARCHAR(255),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    meta JSONB
);
