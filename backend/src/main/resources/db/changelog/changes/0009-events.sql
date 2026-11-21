--liquibase formatted sql

--changeset venyu:0009-events
CREATE TABLE events
(
    id               UUID PRIMARY KEY,
    organizer_id     UUID NOT NULL, -- identity.users.id; modüller arası FK yok
    slug             TEXT NOT NULL UNIQUE,
    title            TEXT NOT NULL,
    description      TEXT,
    visibility       TEXT NOT NULL CHECK (visibility IN ('PUBLIC', 'UNLISTED', 'INVITE_ONLY')),
    status           TEXT NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'CONFIRMED', 'COMPLETED', 'SETTLED', 'CANCELLED')),
    venue_booking_id UUID UNIQUE, -- venue modülü; modüller arası FK yok (ADR-007)
    venue_id         UUID,
    custom_address   TEXT,
    starts_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    version          BIGINT NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at     TIMESTAMP WITH TIME ZONE,
    cancelled_at     TIMESTAMP WITH TIME ZONE,
    CONSTRAINT events_time_order_chk CHECK (ends_at > starts_at),
    -- Konum: ya Venyu mekanı (rezervasyon + mekan) ya serbest adres, tam olarak biri.
    CONSTRAINT events_location_chk CHECK (
        (venue_booking_id IS NOT NULL AND venue_id IS NOT NULL AND custom_address IS NULL)
            OR (venue_booking_id IS NULL AND venue_id IS NULL AND custom_address IS NOT NULL)
        )
);
CREATE INDEX events_organizer_idx ON events (organizer_id);
--rollback DROP TABLE events;
