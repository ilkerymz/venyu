--liquibase formatted sql

--changeset venyu:0003-email-verification-tokens
CREATE TABLE email_verification_tokens
(
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  TEXT NOT NULL UNIQUE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX email_verification_tokens_user_idx ON email_verification_tokens (user_id);
--rollback DROP TABLE email_verification_tokens;