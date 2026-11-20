--liquibase formatted sql

--changeset venyu:0007-venue-bookings
CREATE TABLE venue_bookings
(
    id           UUID PRIMARY KEY,
    slot_id      UUID NOT NULL REFERENCES venue_slots (id),
    venue_id     UUID NOT NULL REFERENCES venues (id) ON DELETE CASCADE, -- slot'tan kopyalanır: sahiplik ve listeleme sorguları join'siz
    organizer_id UUID NOT NULL,                                          -- identity.users.id; modüller arası FK yok (ADR-006)
    status       TEXT NOT NULL CHECK (status IN ('HELD', 'CONFIRMED', 'REJECTED', 'EXPIRED', 'COMPLETED')),
    note         TEXT,
    expires_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    version      BIGINT NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL
);
-- Bir slot'ta aynı anda tek aktif rezervasyon (ADR-006).
CREATE UNIQUE INDEX venue_bookings_one_active_per_slot ON venue_bookings (slot_id) WHERE status IN ('HELD', 'CONFIRMED');
CREATE INDEX venue_bookings_venue_idx ON venue_bookings (venue_id);
CREATE INDEX venue_bookings_organizer_idx ON venue_bookings (organizer_id);
-- venue-4'teki expiry job'ı için: sadece HELD satırları index'lenir.
CREATE INDEX venue_bookings_held_expiry_idx ON venue_bookings (expires_at) WHERE status = 'HELD';
--rollback DROP TABLE venue_bookings;