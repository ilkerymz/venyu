# Venyu

![CI](https://github.com/ilkerymz/venyu/actions/workflows/ci.yml/badge.svg)

Mekanları, organizatörleri ve katılımcıları tek akışta buluşturan sosyal etkinlik platformu.

## Gereksinimler
- Java 21 (Temurin önerilir, SDKMAN ile kurulabilir)
- Docker ve Docker Compose

## Lokal çalıştırma
```bash
docker compose up -d                              # PostgreSQL (port 5433)
export VENYU_JWT_SECRET=$(openssl rand -base64 48) # JWT imza anahtarı (en az 32 karakter)
cd backend
./mvnw spring-boot:run                            # http://localhost:8080
curl localhost:8080/actuator/health
```
IntelliJ'den çalıştırırken `VENYU_JWT_SECRET` değerini Run Configuration → Environment variables alanına ekle.

## Test
```bash
cd backend
./mvnw verify                 # Docker açık olmalı (Testcontainers)
```
Testler kendi JWT secret'ını `src/test/resources/config/application.yml` dosyasından alır; zamanlanmış job'lar testlerde kapalıdır.

## Lokal geliştirme

### Veritabanına bağlanma
```bash
docker compose exec postgres psql -U venyu -d venyu
```

### Bir kullanıcıya VENUE_OWNER rolü verme
Mekan sahibi rolü kendi kendine alınamaz (ADR-006). Lokalde elle verilir:
```bash
docker compose exec postgres psql -U venyu -d venyu -c \
  "INSERT INTO user_roles (user_id, role) SELECT id, 'VENUE_OWNER' FROM users WHERE email = 'e-posta@ornek.com' ON CONFLICT DO NOTHING;"
```
Ardından `POST /auth/refresh` ile yeni token al: roller access token'ın içinde taşınır.

### Geliştirme ortamında e-posta ve SMS
Gerçek gönderim yok; doğrulama link'leri ve OTP kodları uygulama log'una `[DEV EMAIL]` / `[DEV SMS]` önekiyle yazılır.

## Yapı
- `backend/` — Spring Boot modular monolith
- `docs/adr/` — mimari karar kayıtları
