# CLAUDE.md

Venyu: mekan + organizatör + katılımcı buluşturan sosyal etkinlik platformu. Tek geliştirici, öğrenme odaklı.

## Çalışma şekli
- Türkçe açıkla, teknik terimleri İngilizce bırak.
- Büyük değişiklikten önce kısa plan sun ve onay al.
- Sade kod, over-engineering yok. Mevcut görevin ihtiyacından fazlasını kurma.
- Önemli mimari kararlar için `docs/adr/` altında ADR öner.

## Frontend
- Next.js uygulaması `frontend/` altında; kurallar `frontend/CLAUDE.md`'de. Frontend kodunu Claude yazar ve PR açar, kullanıcı review edip merge eder. Backend'de kodu kullanıcı yazar.

## Komutlar
- DB: `docker compose up -d` (repo kökü, PostgreSQL port 5433)
- Çalıştır: `cd backend && ./mvnw spring-boot:run` (`VENYU_JWT_SECRET` env değişkeni gerekli, en az 32 karakter; üretmek için `openssl rand -base64 48`)
- Test: `cd backend && ./mvnw verify`

## Mimari kurallar
- Spring Boot 4.1, Spring Modulith 2.1, Java 21, Maven, PostgreSQL 17.
- Modular monolith. Modüller `com.venyu.<modul>` altında: identity, venue, event, form, registration, payment, discovery, notification.
- Modüller birbirinin tablosuna ve internal sınıflarına erişmez; public API veya event ile haberleşir. `ModularityTests` bunu doğrular.
- Modüller arası event'ler Spring Modulith Event Publication Registry (outbox) üzerinden.
- Şemanın tek sahibi Liquibase: `ddl-auto: validate`. Yeni değişiklik = `db/changelog/changes/NNNN-aciklama.sql` (formatted SQL, rollback'li) + master'a include.
- `open-in-view: false`.
- Hatalar RFC 9457 Problem Details formatında: `ErrorResponseException` türevleri, modülün `exception/` paketinde.
- Kimlik doğrulama: JWT access token (HS256, 15 dk) + opaque refresh token (DB'de SHA-256 hash). Detaylar ADR-003'te.
- Konfigürasyon değerleri `@ConfigurationProperties` record'larıyla okunur (`@Validated`); secret'lar env değişkeninden gelir, repoya yazılmaz.
- Modüller arası event'ler: yayınlayan modülün kök paketinde (public API) `record` olarak tanımlanır; dinleyen modülde `@ApplicationModuleListener` ile (commit sonrası, async, kendi transaction'ında) işlenir. `spring.modulith.events.completion-mode: delete`.
- Tek kullanımlık token'lar (refresh, e-posta doğrulama, şifre sıfırlama): `SecureTokens.generate()` + DB'de `SecureTokens.hash()`; tüketim koşullu atomik UPDATE ile. Detaylar ADR-003 ve ADR-004'te.
- Yan etkisi olan işlemler `GET` ile yapılmaz (örn. e-posta doğrulama link'i frontend sayfası açar, o sayfa `POST` atar).
- API dokümantasyonu: springdoc ile koddan üretilir (code-first). Spec `docs/api/openapi.json` olarak repoda versiyonlanır; `./mvnw verify` dosyayı günceller, CI güncel olmayan spec'te kırmızı olur. API'ye dokunan her PR spec değişikliğini de içerir. Swagger UI: `/swagger-ui.html` (prod'da kapatılacak).
- Modüller arası iletişim: sorgu için senkron public API (modülün kök paketinde arayüz + record, uygulaması `internal`'da), "bir şey oldu" bildirimi için event (ADR-007).

## Kod yerleşimi
Modülün kök paketi (`com.venyu.<modul>`) public API'dir. İç sınıflar `internal/` altında, katmanlara ayrılmış:

```
<modul>/
├── package-info.java   (public API)
└── internal/
    ├── config/         (@Configuration ve @ConfigurationProperties sınıfları)
    ├── domain/         (entity, enum, value object)
    ├── repository/     (Spring Data repository'leri)
    ├── service/        (iş mantığı)
    ├── web/            (controller'lar)
    │   └── dto/        (request/response record'ları)
    └── exception/      (ErrorResponseException türevleri)
```

- Başka bir katman paketinden kullanılan sınıflar (ve constructor'ları) `public`. Sadece kendi paketinde kullanılanlar package-private kalır.
- Bilinçli trade-off: katmanlar arası erişim için `public` kullanılır; modüller arası koruma Spring Modulith'te (`internal` altı başka modüllere kapalı).
- Request/response record'ları `web/dto` altında ve `public`. Service katmanı `web.dto`'yu import etmez; service'ler sade parametre alır, domain nesnesi veya kendi record'unu (örn. `TokenService.IssuedTokens`) döner.
- Entity'ler arası ilişki yerine gerektiğinde düz id alanı kullanılır (örn. `RefreshToken.userId`); FK kısıtı DB'de durur.
- Paket taşımaları IntelliJ **Refactor → Move** ile yapılır.
- Testler, test ettikleri sınıfla aynı paket yapısında durur (`src/test/java/.../internal/<katman>/`).

## İsimlendirme
- Veritabanı: tablo ve kolon adları **snake_case** (`users`, `password_hash`, `email_verified_at`). Tablo adları çoğul; entity'de `@Table(name = ...)` ile açıkça verilir.
- Java: alanlar ve metotlar **camelCase** (`passwordHash`), sınıflar PascalCase.
- Eşleme Spring Boot'un varsayılan naming strategy'si ile otomatik (`passwordHash` → `password_hash`). `@Column(name = ...)` sadece isim gerçekten farklıysa kullanılır.
- Liquibase'in kendi tabloları (`databasechangelog` vb.) bu kurala uymaz; dokunulmaz.

## Test
- Unit + Testcontainers integration (Docker gerekli). Testcontainers imajı `postgres:17` sabit.
- Modül testleri: `@ApplicationModuleTest` + `@Import(TestcontainersConfiguration.class)` (sadece o modül ayağa kalkar).
- `ModularityTests` (`ApplicationModules.verify()`) her zaman yeşil olmalı.
- Teste özel ayarlar `src/test/resources/config/application.yml` içinde (ana `application.yml`'i gölgelemez, sadece içindeki değerleri override eder). Test JWT secret'ı burada.

## Git
- `main` korumalı: her değişiklik branch + PR + yeşil CI ile, squash merge.
- Conventional Commits: feat, fix, docs, ci, chore, refactor, test, build, perf.

## Lombok kuralları
- Entity: sadece `@Getter` ve `@NoArgsConstructor(access = AccessLevel.PROTECTED)`. Gerekirse alan bazında `@Getter(AccessLevel.NONE)`.
- Entity'de yasak: `@Data`, `@Setter`, `@EqualsAndHashCode`, `@ToString`, `@Builder`. Entity'ler factory metot ve davranış metotlarıyla değişir.
- Service / controller: `@RequiredArgsConstructor` (constructor injection, `final` alanlar). İstisna: constructor'da hesaplama gerekiyorsa açık constructor yazılır (örn. `AuthenticationService` dummy hash).
- Loglama: `@Slf4j`.
- DTO / request / response: Java `record`, Lombok değil.
- `pom.xml`'de annotation processor'lar açıkça tanımlı (lombok + spring-modulith-apt). Yeni processor eklenirse bu listeye eklenmeli.
