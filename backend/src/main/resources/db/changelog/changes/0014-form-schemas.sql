--liquibase formatted sql

--changeset venyu:0014-form-schemas
-- Etkinlik başına versiyonlu form şeması. Versiyonlar değişmez: her kaydetme yeni satır (ADR-009).
CREATE TABLE form_schemas
(
    id         UUID PRIMARY KEY,
    event_id   UUID                     NOT NULL, -- event modülü; modüller arası FK yok
    version    INT                      NOT NULL CHECK (version > 0),
    definition JSONB                    NOT NULL, -- {"fields": [...]}
    created_by UUID                     NOT NULL, -- identity modülü (organizatör)
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    -- Eşzamanlı iki kaydetme aynı versiyonu üretemez. event_id ile başladığı için "son versiyon" sorgusunu da karşılar.
    CONSTRAINT form_schemas_event_version_uq UNIQUE (event_id, version)
);
--rollback DROP TABLE form_schemas;
