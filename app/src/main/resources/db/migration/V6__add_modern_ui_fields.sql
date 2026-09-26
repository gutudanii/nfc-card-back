-- V6: Adding Modern UI Fields (Contacts, Portfolio, Avatars, etc.)

DO $$
BEGIN
    -- 1. Track location for device sessions
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'user_sessions' AND column_name = 'location'
    ) THEN
        ALTER TABLE user_sessions ADD COLUMN location VARCHAR(255);
    END IF;

    -- 2. Avatar URL for profile
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'profiles' AND column_name = 'avatar_url'
    ) THEN
        ALTER TABLE profiles ADD COLUMN avatar_url TEXT;
    END IF;

    -- 3. Link organization members back to their specific profile
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'organization_members' AND column_name = 'profile_id'
    ) THEN
        ALTER TABLE organization_members ADD COLUMN profile_id BIGINT REFERENCES profiles(id) ON DELETE SET NULL;
    END IF;
END $$;

-- 4. Rich Profiles (Links and Portfolio)
CREATE TABLE IF NOT EXISTS contact_links (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    url TEXT,
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS portfolio_items (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    item_type VARCHAR(50) NOT NULL, -- PROJECT, SERVICE, EXPERIENCE
    title VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    image_url TEXT,
    external_url TEXT,
    price_text VARCHAR(100),
    display_order INTEGER NOT NULL DEFAULT 0
);
