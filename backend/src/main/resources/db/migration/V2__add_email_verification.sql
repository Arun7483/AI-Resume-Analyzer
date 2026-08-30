-- =====================================================
-- V2 - EMAIL VERIFICATION
-- =====================================================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS verification_token VARCHAR(64);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS verification_expires_at TIMESTAMP(6);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_verification_token
    ON users(verification_token);