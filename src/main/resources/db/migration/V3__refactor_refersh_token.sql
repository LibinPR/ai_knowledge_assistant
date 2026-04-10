-- Remove old token column
ALTER TABLE refresh_tokens
DROP COLUMN token;

-- Add JTI column
ALTER TABLE refresh_tokens
    ADD COLUMN jti VARCHAR(100) NOT NULL;

-- Make JTI unique (VERY IMPORTANT)
ALTER TABLE refresh_tokens
    ADD CONSTRAINT uq_refresh_tokens_jti UNIQUE (jti);

-- Add optimistic locking column
ALTER TABLE refresh_tokens
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Index for faster lookup
CREATE INDEX idx_refresh_tokens_jti
    ON refresh_tokens(jti);