-- V9: Add portfolio_items table and subscription tx_ref tracking

CREATE TABLE IF NOT EXISTS portfolio_items (
    id             BIGSERIAL PRIMARY KEY,
    profile_id     BIGINT       NOT NULL,
    item_type      VARCHAR(50)  NOT NULL DEFAULT 'PROJECT',
    title          VARCHAR(255) NOT NULL,
    category       VARCHAR(100),
    image_url      TEXT,
    external_url   TEXT,
    price_text     VARCHAR(100),
    display_order  INT          NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_portfolio_items_profile ON portfolio_items(profile_id);

-- Add tx_ref tracking to subscriptions so we can verify by txRef lookup
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS tx_ref VARCHAR(100);
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS activated_at TIMESTAMPTZ;
