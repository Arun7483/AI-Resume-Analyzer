ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_reset_token VARCHAR(64);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_reset_expires_at TIMESTAMP(6);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_password_reset_token
    ON users(password_reset_token);