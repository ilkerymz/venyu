--liquibase formatted sql

--changeset venyu:0011-registrations
CREATE TABLE registrations
(
    id             UUID PRIMARY KEY,
    event_id       UUID NOT NULL, -- event modülü; modüller arası FK yok
    ticket_type_id UUID NOT NULL, -- event modülü
    user_id        UUID NOT NULL, -- identity modülü; F2-4'te misafir kaydı için nullable olacak
    status         TEXT NOT NULL CHECK (status IN
                                        ('RESERVED', 'PENDING_THRESHOLD', 'CONFIRMED', 'CHECKED_IN', 'EXPIRED', 'REFUNDED', 'CANCELLED')),
    version        BIGINT NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    cancelled_at   TIMESTAMP WITH TIME ZONE
);
-- Etkinlik başına kişi başı tek aktif kayıt; iptal edilen tekrar kayıt olabilir.
CREATE UNIQUE INDEX registrations_one_active_per_user ON registrations (event_id, user_id)
    WHERE status IN ('RESERVED', 'PENDING_THRESHOLD', 'CONFIRMED', 'CHECKED_IN');
CREATE INDEX registrations_user_idx ON registrations (user_id);
CREATE INDEX registrations_event_idx ON registrations (event_id);
--rollback DROP TABLE registrations;
