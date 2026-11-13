# Venyu

![CI](https://github.com/ilkerymz/venyu/actions/workflows/ci.yml/badge.svg)

Mekanları, organizatörleri ve katılımcıları tek akışta buluşturan sosyal etkinlik platformu.

## Gereksinimler
- Java 21 (Temurin önerilir, SDKMAN ile kurulabilir)
- Docker ve Docker Compose

## Lokal çalıştırma
```bash
docker compose up -d          # PostgreSQL (port 5433)
cd backend
./mvnw spring-boot:run        # http://localhost:8080
curl localhost:8080/actuator/health
```

## Test
```bash
cd backend
./mvnw verify                 # Docker açık olmalı (Testcontainers)
```

## Yapı
- `backend/` — Spring Boot modular monolith
- `docs/adr/` — mimari karar kayıtları
