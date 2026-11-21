--liquibase formatted sql

--changeset venyu:0012-guest-registrations
-- Organizatör "sadece hesabı olanlar katılabilir" diyebilir; varsayılan: misafir kaydı açık.
ALTER TABLE events ADD COLUMN requires_account BOOLEAN NOT NULL DEFAULT false;

-- Misafir kaydı: kimlik ya user_id ya guest_email (tam olarak biri).
ALTER TABLE registrations ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE registrations
    ADD COLUMN guest_email             TEXT,
    ADD COLUMN guest_name              TEXT,
    ADD COLUMN confirmation_token_hash TEXT,
    ADD COLUMN expires_at              TIMESTAMP WITH TIME ZONE;
ALTER TABLE registrations ADD CONSTRAINT registrations_one_identity_chk
    CHECK ((user_id IS NULL) <> (guest_email IS NULL));
ALTER TABLE registrations ADD CONSTRAINT registrations_guest_name_chk
    CHECK (guest_email IS NULL OR guest_name IS NOT NULL);
-- RESERVED kayıt süresiz kalamaz: expiry job onu bulabilmeli.
ALTER TABLE registrations ADD CONSTRAINT registrations_reserved_expiry_chk
    CHECK (status <> 'RESERVED' OR expires_at IS NOT NULL);

-- Etkinlik başına e-posta başı tek aktif misafir kaydı (registrations_one_active_per_user ile aynı durumlar).
CREATE UNIQUE INDEX registrations_one_active_per_guest ON registrations (event_id, guest_email)
    WHERE status IN ('RESERVED', 'PENDING_THRESHOLD', 'CONFIRMED', 'CHECKED_IN');
CREATE UNIQUE INDEX registrations_confirmation_token_idx ON registrations (confirmation_token_hash);
-- Expiry job'ın sorgusu: sadece RESERVED satırlar, küçük bir index.
CREATE INDEX registrations_reserved_expiry_idx ON registrations (expires_at) WHERE status = 'RESERVED';
--rollback DROP INDEX registrations_reserved_expiry_idx;
--rollback DROP INDEX registrations_confirmation_token_idx;
--rollback DROP INDEX registrations_one_active_per_guest;
--rollback ALTER TABLE registrations DROP CONSTRAINT registrations_reserved_expiry_chk;
--rollback ALTER TABLE registrations DROP CONSTRAINT registrations_guest_name_chk;
--rollback ALTER TABLE registrations DROP CONSTRAINT registrations_one_identity_chk;
--rollback ALTER TABLE registrations DROP COLUMN expires_at, DROP COLUMN confirmation_token_hash, DROP COLUMN guest_name, DROP COLUMN guest_email;
--rollback ALTER TABLE registrations ALTER COLUMN user_id SET NOT NULL;
--rollback ALTER TABLE events DROP COLUMN requires_account;
