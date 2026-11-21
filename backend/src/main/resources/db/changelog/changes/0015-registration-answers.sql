--liquibase formatted sql

--changeset venyu:0015-registration-answers
-- Form cevapları (ADR-009): sadece görünen alanlar, doğrulandığı form versiyonuyla. Formsuz etkinlikte {} ve NULL.
ALTER TABLE registrations
    ADD COLUMN answers      JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN form_version INT;
--rollback ALTER TABLE registrations DROP COLUMN form_version, DROP COLUMN answers;
