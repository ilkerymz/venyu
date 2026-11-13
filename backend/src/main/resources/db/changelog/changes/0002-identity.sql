--liquibase formatted sql

--changeset venyu:0002-identity-users
CREATE TABLE users
(
  id                UUID PRIMARY KEY,
  email             TEXT NOT NULL,
  password_hash     TEXT NOT NULL,
  email_verified_at TIMESTAMP WITH TIME ZONE,
  display_name      TEXT NOT NULL,
  phone             TEXT,
  phone_verified_at TIMESTAMP WITH TIME ZONE,
  created_at        TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX users_email_uq ON users (lower(email));
CREATE UNIQUE INDEX users_verified_phone_uq ON users (phone) WHERE phone_verified_at IS NOT NULL;
--rollback DROP TABLE users;

--changeset venyu:0002-identity-user-roles
CREATE TABLE user_roles
(
  user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  role    TEXT NOT NULL CHECK (role IN ('PARTICIPANT', 'ORGANIZER', 'VENUE_OWNER')),
  PRIMARY KEY (user_id, role)
);
--rollback DROP TABLE user_roles;

--changeset venyu:0002-identity-refresh-tokens
CREATE TABLE refresh_tokens
(
  id         UUID PRIMARY KEY,
  user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  token_hash TEXT NOT NULL UNIQUE,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
  revoked_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX refresh_tokens_user_idx ON refresh_tokens (user_id);
--rollback DROP TABLE refresh_tokens;

--changeset venyu:0002-identity-phone-otp
CREATE TABLE phone_otp_challenges
(
  id          UUID PRIMARY KEY,
  user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  phone       TEXT NOT NULL,
  code_hash   TEXT NOT NULL,
  expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
  attempts    INT NOT NULL DEFAULT 0,
  consumed_at TIMESTAMP WITH TIME ZONE,
  created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX phone_otp_challenges_user_created_idx ON phone_otp_challenges (user_id, created_at);
--rollback DROP TABLE phone_otp_challenges;
