--liquibase formatted sql

--changeset venyu:0005-venues
CREATE TABLE venues
(
    id          UUID PRIMARY KEY,
    owner_id    UUID NOT NULL, -- identity.users.id; modüller arası FK yok (ADR-006)
    name        TEXT NOT NULL,
    description TEXT,
    address     TEXT NOT NULL,
    district    TEXT NOT NULL,
    city        TEXT NOT NULL,
    capacity    INT  NOT NULL CHECK (capacity > 0),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX venues_owner_idx ON venues (owner_id);
CREATE INDEX venues_district_idx ON venues (district);
--rollback DROP TABLE venues;