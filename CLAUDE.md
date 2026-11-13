# CLAUDE.md

Venyu: mekan + organizatör + katılımcı buluşturan sosyal etkinlik platformu. Tek geliştirici, öğrenme odaklı.

## Çalışma şekli
- Türkçe açıkla, teknik terimleri İngilizce bırak.
- Büyük değişiklikten önce kısa plan sun ve onay al.
- Sade kod, over-engineering yok. Mevcut görevin ihtiyacından fazlasını kurma.
- Önemli mimari kararlar için `docs/adr/` altında ADR öner.

## Komutlar
- DB: `docker compose up -d` (repo kökü, PostgreSQL port 5433)
- Çalıştır: `cd backend && ./mvnw spring-boot:run`
- Test: `cd backend && ./mvnw verify`

## Mimari kurallar
- Spring Boot 4.1, Spring Modulith 2.1, Java 21, Maven, PostgreSQL 17.
- Modular monolith. Modüller `com.venyu.<modul>` altında: identity, venue, event, form, registration, payment, discovery, notification.
- Modüller birbirinin tablosuna ve internal sınıflarına erişmez; public API veya event ile haberleşir. `ModularityTests` bunu doğrular.
- Modüller arası event'ler Spring Modulith Event Publication Registry (outbox) üzerinden.
- Şemanın tek sahibi Liquibase: `ddl-auto: validate`. Yeni değişiklik = `db/changelog/changes/NNNN-aciklama.sql` (formatted SQL, rollback'li) + master'a include.
- `open-in-view: false`.

## Git
- `main` korumalı: her değişiklik branch + PR + yeşil CI ile.
- Conventional Commits: feat, fix, docs, ci, chore, refactor, test.

## Lombok kuralları
- Entity: sadece `@Getter` ve `@NoArgsConstructor(access = AccessLevel.PROTECTED)`. Gerekirse alan bazında `@Getter(AccessLevel.NONE)`.
- Entity'de yasak: `@Data`, `@Setter`, `@EqualsAndHashCode`, `@ToString`, `@Builder`. Entity'ler factory metot ve davranış metotlarıyla değişir.
- Service / controller: `@RequiredArgsConstructor` (constructor injection, `final` alanlar).
- Loglama: `@Slf4j`.
- DTO / request / response: Java `record`, Lombok değil.
