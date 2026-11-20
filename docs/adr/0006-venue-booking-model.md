# ADR-006: Venue modülü: slot modeli, modüller arası referanslar ve rezervasyon eşzamanlılığı

- **Durum:** Kabul edildi
- **Tarih:** 2026-11-21

## Bağlam
Venue modülü üç şeyi yönetir: mekan profili, mekanın müsait zaman dilimleri (slot) ve organizatörlerin bu slot'lar için yaptığı rezervasyonlar (`VenueBooking`: HELD → CONFIRMED / REJECTED / EXPIRED). Mekan sahibi (`VENUE_OWNER`) mekanını ve slot'larını yönetir, organizatör (`ORGANIZER`) slot tutar, sahip onaylar veya reddeder.

Fiyat modeli (sabit kira / kişi başı minimum / gelir payı) F0 görüşmeleri sonuçlanmadığı için henüz belli değil.

## Değerlendirilen seçenekler

**Slot modeli**
1. **Açık slot'lar:** Sahip belirli zaman dilimleri açar, organizatör birini seçer. Basit, anlaşılır.
2. **Müsaitlik kuralları:** Sahip tekrar eden kurallar tanımlar ("hafta içi 18–23"), organizatör aralıkta istediği saati seçer. Esnek ama tekrar eden kurallar, istisnalar ve çakışma hesabı karmaşık.

**Başka modüllerin verisine referans** (`owner_id`, `organizer_id` → identity'deki `users`)
1. **Foreign key:** DB seviyesinde bütünlük. Ama venue tablolarını identity'nin tablosuna bağlar; modülü ileride ayrı servise çıkarmayı zorlaştırır.
2. **Sadece UUID:** Modüller DB'de de bağımsız kalır. Bütünlük uygulama tarafında (kimlik JWT'den gelir).

**Eşzamanlılık**
- Aynı mekanda çakışan slot'lar: uygulamada kontrol (check-then-act yarışına açık) veya PostgreSQL **exclusion constraint**.
- Aynı slot'u iki organizatörün tutması: uygulamada kontrol veya **partial unique index**.
- Durum geçişlerinin çakışması (sahip onaylarken job'un süreyi doldurması): pessimistic lock veya **optimistic locking (`@Version`)**.

## Karar
- **Açık slot modeli** (seçenek 1). Müsaitlik kuralları mekanlardan gerçek ihtiyaç gelirse değerlendirilir.
- **Modüller arası FK yok:** `owner_id`, `organizer_id` sadece UUID. Modül içi ilişkilerde (slot → venue, booking → slot) FK kullanılır.
- **Yetkilendirme:** Rol kontrolü `@PreAuthorize` ile (method security). Sahiplik kontrolü servis katmanında (`venue.isOwnedBy(userId)`); başkasının mekanını değiştirme `403`. Okuma uç noktaları (`GET /venues/**`) herkese açık; bu kural `SecurityConfig`'te (identity modülü) tanımlı. Kimlik doğrulama identity'nin sorumluluğu olduğu için bu bilinçli bir istisna.
- **Çakışan slot'lar:** `EXCLUDE USING gist (venue_id WITH =, tstzrange(starts_at, ends_at) WITH &&)` (`btree_gist` extension'ı gerekir).
- **Bir slot'ta tek aktif rezervasyon:** `UNIQUE (slot_id) WHERE status IN ('HELD', 'CONFIRMED')`. İhlal `409`.
- **Durum geçişleri:** `VenueBooking` üzerinde `@Version` ile optimistic locking. Çakışma `409`. Toplu güncelleyen job, sorgusunda `version = version + 1` yapar.
- **Hold süresi:** 24 saat. Dakikada bir çalışan job süresi dolan hold'ları `EXPIRED` yapar; çok instance için **ShedLock**.
- **Fiyat:** Şimdilik modellenmez; F3'te (ödeme) eklenir.

## Sonuçlar
- ✅ Çakışma kuralları DB'de; uygulama kodundaki bir hata veya yarış durumu bunları delemez.
- ✅ Venue modülü identity'ye ne kodda ne DB'de bağımlı; sadece JWT'deki kimlik ve rolleri kullanır.
- ✅ Optimistic locking, çakışmanın nadir olduğu durum geçişlerinde kilit maliyeti olmadan doğruluk sağlar.
- ⚠️ FK olmadığı için silinmiş bir kullanıcının mekanı "sahipsiz" kalabilir. Kullanıcı silme akışı geldiğinde event ile ele alınacak.
- ⚠️ Herkese açık yolların listesi identity'deki `SecurityConfig`'te; her yeni modül public uç nokta eklediğinde orayı da güncellemek gerekir.
- ⚠️ Slot modeli ve 24 saatlik hold süresi varsayım; F0 mekan görüşmeleriyle doğrulanmalı.
- ⚠️ `btree_gist` extension'ı gerekiyor; managed PostgreSQL seçerken desteklendiği kontrol edilmeli (yaygın sağlayıcılarda destekleniyor).