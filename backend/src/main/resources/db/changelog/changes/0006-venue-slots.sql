--liquibase formatted sql

--changeset venyu:0006-venue-slots
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE venue_slots
(
    id         UUID PRIMARY KEY,
    venue_id   UUID NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
    starts_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT venue_slots_time_order_chk CHECK (ends_at > starts_at),
    -- Aynı mekanda zaman aralıkları kesişemez (ADR-006).
    CONSTRAINT venue_slots_no_overlap EXCLUDE USING gist (
        venue_id WITH =,
        tstzrange(starts_at, ends_at) WITH &&
    )
);
--rollback DROP TABLE venue_slots;