--liquibase formatted sql

--changeset venyu:0008-shedlock
-- ShedLock'un kendi tablosu (şema ShedLock dokümantasyonundan; isimlendirme kuralımızın dışında).
CREATE TABLE shedlock
(
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
--rollback DROP TABLE shedlock;