-- V7: Phase 2 — visible flag on contact_links, my-orders query, nfc my-cards

DO $$
BEGIN
    -- Add visible column to contact_links (not in original V6)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'contact_links' AND column_name = 'visible'
    ) THEN
        ALTER TABLE contact_links ADD COLUMN visible BOOLEAN NOT NULL DEFAULT true;
    END IF;

    -- Add delivered_at to orders for order history display
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'delivered_at'
    ) THEN
        ALTER TABLE orders ADD COLUMN delivered_at TIMESTAMP WITH TIME ZONE;
    END IF;
END $$;
