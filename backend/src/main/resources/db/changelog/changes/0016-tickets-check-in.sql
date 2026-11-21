--liquibase formatted sql

--changeset venyu:0016-tickets-check-in
-- QR bilet kodu (onaylı kayıtta dolu), misafirin "yönet" token'ı (hash) ve giriş bilgisi.
ALTER TABLE registrations
    ADD COLUMN ticket_code       TEXT,
    ADD COLUMN manage_token_hash TEXT,
    ADD COLUMN checked_in_at     TIMESTAMP WITH TIME ZONE,
    ADD COLUMN checked_in_by     UUID; -- identity modülü (girişi yapan organizatör)

-- F2-6 öncesi onaylanmış kayıtlara kod (gen_random_uuid PostgreSQL 13+ yerleşik, 122 bit rastgele).
UPDATE registrations
SET ticket_code = replace(gen_random_uuid()::text, '-', '')
WHERE status IN ('CONFIRMED', 'CHECKED_IN');

CREATE UNIQUE INDEX registrations_ticket_code_idx ON registrations (ticket_code);
CREATE UNIQUE INDEX registrations_manage_token_idx ON registrations (manage_token_hash);
ALTER TABLE registrations ADD CONSTRAINT registrations_ticket_code_chk
    CHECK (status NOT IN ('CONFIRMED', 'CHECKED_IN') OR ticket_code IS NOT NULL);
ALTER TABLE registrations ADD CONSTRAINT registrations_checked_in_chk
    CHECK ((status = 'CHECKED_IN') = (checked_in_at IS NOT NULL));
--rollback ALTER TABLE registrations DROP CONSTRAINT registrations_checked_in_chk;
--rollback ALTER TABLE registrations DROP CONSTRAINT registrations_ticket_code_chk;
--rollback DROP INDEX registrations_manage_token_idx;
--rollback DROP INDEX registrations_ticket_code_idx;
--rollback ALTER TABLE registrations DROP COLUMN checked_in_by, DROP COLUMN checked_in_at, DROP COLUMN manage_token_hash, DROP COLUMN ticket_code;
