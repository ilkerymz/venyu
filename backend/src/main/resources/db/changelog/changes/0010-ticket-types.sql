--liquibase formatted sql

--changeset venyu:0010-ticket-types
-- Mekanlı etkinlikte toplam bilet kapasitesi üst sınırı (mekan kapasitesinin anlık kopyası); adresli etkinlikte NULL.
ALTER TABLE events ADD COLUMN capacity_limit INT CHECK (capacity_limit > 0);

CREATE TABLE ticket_types
(
    id          UUID PRIMARY KEY,
    event_id    UUID           NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    name        TEXT           NOT NULL,
    description TEXT,
    price       NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    currency    TEXT           NOT NULL DEFAULT 'TRY',
    capacity    INT CHECK (capacity > 0), -- NULL = sınırsız (ADR-007)
    sold        INT            NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    -- Son savunma hattı: uygulama ne yaparsa yapsın satılan, kapasiteyi aşamaz.
    CONSTRAINT ticket_types_sold_chk CHECK (sold >= 0 AND (capacity IS NULL OR sold <= capacity))
);
CREATE INDEX ticket_types_event_idx ON ticket_types (event_id);
--rollback DROP TABLE ticket_types; ALTER TABLE events DROP COLUMN capacity_limit;
