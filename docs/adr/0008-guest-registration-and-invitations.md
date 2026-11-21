# ADR-008: Misafir kaydı ve davetler

- **Durum:** Kabul edildi
- **Tarih:** 2026-11-21

## Bağlam
Venyu'da katılımcının hesabı olmak zorunda değil (context 4.1, seviye 0: ad + e-posta). Organizatör ise etkinliğe göre üç şey isteyebilir: herkes kayıt olabilsin, sadece hesabı olanlar kayıt olabilsin (`requiresAccount`) veya sadece davet ettikleri katılabilsin (`INVITE_ONLY`). F2-3'te sadece üye kaydı vardı. Envanter event modülünde (ADR-007), kayıt registration modülünde.

İki risk öne çıktı:
- **Capacity griefing:** doğrulanmamış e-postalarla kayıt açıksa bir bot sahte adreslerle etkinliği saniyeler içinde "tükendi" yapabilir.
- **Davet link'inin iletilmesi:** link başkasına giderse aynı davetle birden fazla koltuk alınmamalı.

## Değerlendirilen seçenekler

**Misafirin e-postası**
1. Doğrulamadan kaydet (CONFIRMED): en basit, ama griefing'e açık ve organizatör sahte kayıtları ayıramaz.
2. **Koltuğu süreli tut (RESERVED, 30 dk), e-postadaki link'le onayla:** sahte adresle tutulan koltuk 30 dk sonra geri döner.
3. CAPTCHA / rate limit: bot'u yavaşlatır ama e-postanın gerçek olduğunu kanıtlamaz; 2'nin yerine değil yanına eklenir.

**Davetli misafir**
1. Normal misafir gibi e-posta onayı.
2. **Onay yok:** davet link'i zaten o adrese gönderildi; token'a sahip olmak e-postaya sahip olmanın kanıtı sayılır.

**Kuralların yeri ("hesap gerekli", "sadece davetli")**
1. Registration modülü etkinlik ayarlarını okur ve kontrol eder: kural envanterden ayrılır, iki modül aynı ayarı yorumlar.
2. **Event modülü envanterle birlikte kontrol eder:** `TicketInventory.reserve(eventId, ticketTypeId, ReservationContext)`, `ReservationContext(asGuest, withInvitation)`.

**Onay token'ının yeri**
1. Ayrı tablo (identity'deki e-posta token'ları gibi).
2. **`registrations` üzerinde kolon:** bir kaydın tek bir onay token'ı var; ayrı yaşam döngüsü yok.

## Karar
- **Kimlik:** `registrations.user_id` nullable; misafir için `guest_email` (lowercase normalize) + `guest_name`. DB'de `CHECK ((user_id IS NULL) <> (guest_email IS NULL))`: tam olarak bir kimlik. Etkinlik başına e-posta başı tek aktif kayıt (partial unique index, üyedeki ile aynı durumlar).
- **Misafir akışı:** `RESERVED` + `expires_at` (30 dk) + `confirmation_token_hash` (32 byte opaque token, SHA-256; ADR-004 deseni). Onaylanınca `CONFIRMED`, hash silinir (tek kullanım). Süresi dolanları `ExpiredRegistrationJob` (ShedLock, 1 dk, tur başına 100) `EXPIRED` yapıp koltuğu bırakır. Süresi dolan kaydın hash'i silinmez: geç tıklayan "geçersiz link" yerine "süre doldu" görür. Onay ile job yarışını `@Version` çözer.
- **Davetler:** `invitations` tablosu (registration modülü), `(event_id, email)` unique, token hash'li ve tek kullanımlık (`PENDING → ACCEPTED`, `@Version`). Toplu gönderim idempotent: davetli e-postalar atlanır. Davetli üye veya misafir olarak kabul eder; misafir kabulü doğrudan `CONFIRMED`. INVITE_ONLY etkinlik slug'la görünmediği için davetli etkinliği `POST /invitations/preview` ile görür (token URL'de değil body'de).
- **Kabul sırası:** önce davet `ACCEPTED` yapılıp flush edilir, sonra koltuk ayrılır. Sonraki adım hata verirse (tükendi, zaten kayıtlı, hesap gerekli) transaction davet dahil her şeyi geri alır. Aynı daveti iki kişi aynı anda kabul ederse biri optimistic hata alır (409).
- **Kurallar event modülünde:** `ReservationContext` ile; ret `TicketReservation.NotAvailable(reason)`. Etkinliğe `requires_account` kolonu. Yeni senkron API `EventLookup` (`EventSummary`, `TicketOffer`): organizatör kontrolü, bildirim metni, davet önizlemesi.
- **Bildirim:** `GuestConfirmationRequested` ve `InvitationSent` registration kök paketinde; notification dinler (şimdilik `[DEV EMAIL]`). Ham token sadece event'te taşınır (ADR-004 ile aynı kabul).

## Sonuçlar
- ✅ Sahte e-postayla tutulan koltuk en fazla 30 dk kilitli kalır; organizatörün listesindeki misafirler gerçek adresler.
- ✅ Görünürlük ve hesap kuralları tek yerde, envanterle aynı transaction'da.
- ✅ Davet tek kullanımlık; başarısız kabul daveti harcamaz.
- ⚠️ Davet link'i iletilirse ilk tıklayan koltuğu alır (en fazla bir koltuk). E-posta eşleşmesi zorunlu değil.
- ⚠️ Misafir endpoint'i "bu e-posta zaten kayıtlı" diye 409 dönüyor: bir e-postanın etkinliğe kayıtlı olup olmadığı dışarıdan anlaşılabilir.
- ⚠️ Misafir uçlarında rate limiting yok; 30 dk'lık tutma süresi griefing'i sınırlar ama engellemez.
- ⚠️ Misafir kendi kaydını iptal edemiyor (F2-6'da bilet e-postasındaki "yönet" link'i ile), davet geri alınamıyor.
- ⚠️ `SecureTokens` identity ile registration'da kopya (modüller internal sınıf paylaşamaz); üçüncü kullanıcıda ortak pakete taşınacak.
