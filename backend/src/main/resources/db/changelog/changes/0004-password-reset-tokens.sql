--liquibase formatted sql

--changeset venyu:0004-password-reset-tokens
CREATE TABLE password_reset_tokens
(
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  TEXT NOT NULL UNIQUE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX password_reset_tokens_user_idx ON password_reset_tokens (user_id);
--rollback DROP TABLE password_reset_tokens;