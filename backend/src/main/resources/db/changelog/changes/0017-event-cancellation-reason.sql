--liquibase formatted sql

--changeset venyu:0017-event-cancellation-reason
-- Organizatörün iptal nedeni (opsiyonel); katılımcılara giden iptal bildiriminde yer alır.
ALTER TABLE events ADD COLUMN cancellation_reason TEXT;
--rollback ALTER TABLE events DROP COLUMN cancellation_reason;
