-- Add platform_role column to users table
-- Valid values: NULL (regular user), SUPER_ADMIN, SUPPORT_ADMIN, OPERATIONS_ADMIN
ALTER TABLE users ADD COLUMN IF NOT EXISTS platform_role VARCHAR(50) DEFAULT NULL;

CREATE INDEX IF NOT EXISTS idx_users_platform_role ON users(platform_role)
    WHERE platform_role IS NOT NULL;
