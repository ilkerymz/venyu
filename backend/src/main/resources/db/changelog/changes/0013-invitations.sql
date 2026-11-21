--liquibase formatted sql

--changeset venyu:0013-invitations
CREATE TABLE invitations
(
    id          UUID PRIMARY KEY,
    event_id    UUID NOT NULL, -- event modülü; modüller arası FK yok
    email       TEXT NOT NULL, -- lowercase normalize edilmiş
    token_hash  TEXT NOT NULL, -- ham token saklanmaz (ADR-004)
    status      TEXT NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED')),
    invited_by  UUID NOT NULL, -- identity modülü (organizatör)
    version     BIGINT NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT invitations_accepted_at_chk CHECK ((status = 'ACCEPTED') = (accepted_at IS NOT NULL))
);
-- Aynı kişiye aynı etkinlik için tek davet. event_id ile başladığı için listeleme sorgusunu da karşılar.
CREATE UNIQUE INDEX invitations_event_email_idx ON invitations (event_id, email);
CREATE UNIQUE INDEX invitations_token_hash_idx ON invitations (token_hash);
--rollback DROP TABLE invitations;
