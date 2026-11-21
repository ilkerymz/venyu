# ADR-007: Etkinlik modeli, konum seçenekleri ve modüller arası senkron API

- **Durum:** Kabul edildi
- **Tarih:** 2026-11-21

## Bağlam
F2'de etkinlik, bilet ve kayıt akışları geliyor. Venyu tamamen dinamik bir organizasyon uygulaması olarak tasarlanıyor: etkinlik bir Venyu mekanında veya serbest bir adreste olabilir; ücretsiz veya ücretli olabilir; kayıt formu olabilir ya da hiç olmayabilir; katılımcı sayısı sınırlı veya sınırsız olabilir. Event modülü, organizatörün onaylı mekan rezervasyonunu doğrulamak için venue modülünün verisine ihtiyaç duyuyor, ama modüller birbirinin tablosuna dokunamaz (ADR-001, ADR-006).

## Değerlendirilen seçenekler

**Etkinliğin yeri**
1. Her etkinlik bir onaylı `VenueBooking`'e bağlı: tutarlı ama ev, park, kendi ofisi gibi kullanımları dışlıyor.
2. **Mekan rezervasyonu veya serbest adres**: ikisinden tam olarak biri.

**Venue verisine erişim**
1. **Senkron public API:** venue kök paketinde bir arayüz (`VenueBookings`); event modülü çağırır, her zaman güncel veriyi okur.
2. Event ile read model: venue `BookingConfirmed` yayınlar, event modülü kopya tutar. Daha gevşek bağlı ama kopyanın senkron tutulması gerekir.

**Bilet envanteri** (F2-2/F2-3)
1. **Event modülünde** (`TicketType.sold`), registration public API ile ayırır/bırakır.
2. Registration modülünde: tanım ile sayaç ayrı yerlerde olur.

**Para**
1. Kuruş cinsinden `long`.
2. **`BigDecimal`** + `NUMERIC(12,2)` + para birimi.

## Karar
- **Konum:** `venue_booking_id` + `venue_id` **veya** `custom_address`; DB'de `CHECK` ile tam olarak biri. Zaman her iki durumda `events` tablosunda tutulur (mekanlıda rezervasyondan kopyalanır). Bir rezervasyona tek etkinlik (`UNIQUE (venue_booking_id)`).
- **Modüller arası iletişim kuralı:** *sorgu için senkron public API, "bir şey oldu" bildirimi için event.* İlk public API: `com.venyu.venue.VenueBookings.findConfirmed(bookingId, organizerId)` → sadece CONFIRMED ve organizatöre ait rezervasyonu döner (`ConfirmedVenueBooking` record).
- **Yaşam döngüsü (F2):** `DRAFT → PUBLISHED`, `DRAFT | PUBLISHED → CANCELLED`. `CONFIRMED` (eşik), `COMPLETED`, `SETTLED` F3'te; DB'deki `CHECK` listesinde şimdiden var. Geçişler domain'de, eşzamanlılık `@Version` ile.
- **Görünürlük:** `PUBLIC` (feed'de, F4), `UNLISTED` (sadece link), `INVITE_ONLY` (davet, F2-4). Paylaşım link'i tahmin edilemeyen 10 karakterlik `slug` (≈ 2⁵⁰ olasılık). DRAFT ve INVITE_ONLY etkinlik, organizatör dışındakilere `404` (varlığı bile açığa vurulmaz).
- **Bilet ve kapasite (F2-2):** her etkinliğin en az bir bilet türü vardır (tanımlanmazsa otomatik ücretsiz "Genel Katılım"); kapasite `NULL` = sınırsız; mekanlı etkinlikte toplam kapasite mekan kapasitesini aşamaz ve sınırsız olamaz. Envanter event modülünde; overselling `UPDATE ... WHERE capacity IS NULL OR sold < capacity`.
- **Para:** `BigDecimal`, `NUMERIC(12,2)`, `currency` kolonu; karşılaştırma `compareTo`; yuvarlama `HALF_EVEN`. Ücretli bilet F2'de tanımlanabilir, satışı F3'te (ödeme) açılır.
- **Form:** opsiyonel (F2-5); kural dili JEXL (`JexlPermissions.RESTRICTED`), aynı kurallar tarayıcıda JS JEXL ile değerlendirilecek.

## Sonuçlar
- ✅ Mekanlı ve mekansız etkinlikler tek modelde; sorgular (zaman, durum) tek tabloda.
- ✅ Venue'nun iç yapısı event'ten gizli; sözleşme tek bir arayüz.
- ✅ Envanter tek modülde, tanımıyla birlikte; overselling kuralı tek yerde.
- ⚠️ Event → venue senkron bağımlılığı: venue modülü servise ayrılırsa bu çağrı ağ üzerinden yapılır (gecikme, hata yönetimi).
- ⚠️ Registration → event envanter çağrısı bugün aynı transaction'da atomik; modüller ayrı servis olursa saga gerekir.
- ⚠️ İptal edilen mekanlı etkinliğin rezervasyonu yeni etkinlikte kullanılamaz (unique). Rezervasyonun serbest bırakılması / mekana bildirim ayrı bir iş.
- ✅ (F2-7) Etkinlik iptali `EventCancelled` olarak yayınlanır; registration aktif kayıtları (RESERVED, CONFIRMED) iptal eder ve katılımcı başına bildirim yayınlar (idempotent, outbox). Kayıt ↔ iptal yarışı `TicketInventory.reserve`'de etkinlik satırının `FOR SHARE` ile okunmasıyla kapatıldı. İade F3'te.
- ⚠️ İptal akışının otomatik testleri yok (bilinçli teknik borç, F3'ten önce).
