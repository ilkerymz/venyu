# ADR-004: E-posta token'ları ve modüller arası bildirim event'leri

- **Durum:** Kabul edildi
- **Tarih:** 2026-11-20

## Bağlam
Identity modülü iki yeni akış ekliyor: e-posta doğrulama ve şifre sıfırlama. İkisi de kullanıcıya içinde tek kullanımlık bir link olan e-posta gönderilmesini gerektiriyor. Mimari kurala göre (ADR-001) bildirimleri **notification** modülü gönderir; modüller birbirinin iç sınıflarını çağırmaz, public API veya event ile haberleşir. Bu, projedeki ilk modüller arası event olacak.

Token'lar refresh token'larla aynı güvenlik prensibine tabi: DB'de ham değer değil, sadece hash tutulur (ADR-003).

## Değerlendirilen seçenekler

**Token saklama**
1. **Tek tablo + `purpose` kolonu:** Ortak yaşam döngüsü tek yerde, yeni amaçlar için sadece enum değeri eklenir.
2. **Amaç başına ayrı tablo:** Her tablo tek bir işi yapar, okuması ve ileride ayrı ayrı değiştirmesi kolay. Bedeli: benzer iki tablo ve entity.

**E-postayı kim gönderir**
1. **Identity doğrudan gönderir:** Basit, ama bildirim sorumluluğu identity'ye sızar ve ADR-001'deki modül sınırı kuralına aykırı.
2. **Identity event yayınlar, notification dinleyip gönderir:** Sorumluluklar ayrı kalır. Spring Modulith Event Publication Registry sayesinde event, iş transaction'ı ile birlikte `event_publication` tablosuna yazılır (outbox): kayıt rollback olursa e-posta gitmez, notification o an başarısız olursa event kaybolmaz.

**Ham token'ın notification'a ulaşması**
1. **Event, ham token'ı taşır:** Basit. Ama event `event_publication` tablosunda serialize edildiği için ham token geçici olarak DB'de düz metin durur.
2. **Event sadece userId taşır, notification token'ı identity'den ister:** DB'de ham token kalmaz, ama token üretimini notification tetiklemiş olur; sorumluluklar karışır ve modüller arasında senkron bağımlılık oluşur.

## Karar
- **Ayrı tablolar:** `email_verification_tokens` ve `password_reset_tokens`. Kolonlar: `id`, `user_id` (FK, `ON DELETE CASCADE`), `token_hash` (unique), `expires_at`, `consumed_at`, `created_at`.
- **Token:** 32 byte `SecureRandom`, base64url. DB'de SHA-256 hex hash (`TokenService.hash` ile aynı yöntem). Süreler: e-posta doğrulama 24 saat, şifre sıfırlama 30 dakika.
- **Tek kullanım:** Koşullu ve atomik update: `UPDATE ... SET consumed_at = now WHERE token_hash = ? AND consumed_at IS NULL AND expires_at > now`. Etkilenen satır 1 ise geçerli, 0 ise reddedilir (ADR-003'teki desenin aynısı).
- **Event'ler:** Identity modülünün public API'sinde (kök paket `com.venyu.identity`) record olarak tanımlanır, örn. `EmailVerificationRequested(UUID userId, String email, String displayName, String token)`. Notification modülü `@ApplicationModuleListener` ile dinler. Geliştirme ortamında e-posta log'a yazılır.
- **Ham token event'te taşınır** (seçenek 1). Riski azaltmak için:
  - Kısa ömür (24 saat / 30 dakika) ve tek kullanım.
  - `spring.modulith.events.completion-mode: delete`: listener başarıyla tamamlandığında event satırı tablodan silinir; ham token yalnızca işlenene kadar DB'de durur.
  - Token tablolarında yine sadece hash tutulur.
- **Davranışlar:**
  - Kayıttan sonra doğrulama e-postası otomatik gönderilir. Doğrulanmamış kullanıcı login olabilir; doğrulama şimdilik hiçbir işlemi kilitlemez.
  - Şifre sıfırlama isteği, e-posta kayıtlı olmasa da her zaman `202` döner (user enumeration'a karşı).
  - Şifre sıfırlandığında kullanıcının tüm refresh token'ları revoke edilir.

## Sonuçlar
- ✅ Bildirim sorumluluğu notification modülünde; identity, e-postanın nasıl gönderildiğini bilmez. Gerçek bir e-posta sağlayıcısına geçiş sadece notification'ı etkiler.
- ✅ Outbox sayesinde "kayıt oldu ama e-posta hiç gitmedi" veya "kayıt geri alındı ama e-posta gitti" durumları oluşmaz.
- ✅ Token tabloları sızsa bile ham token'lar ele geçmez.
- ⚠️ Ham token, event işlenene kadar `event_publication` tablosunda düz metin durur. Listener hata alırsa satır silinmez ve token süresi dolana kadar orada kalabilir. DB erişimi olan biri bu kısa pencerede token'ı okuyabilir.
- ⚠️ `completion-mode: delete` ile tamamlanmış event'lerin denetim izi (audit trail) tutulmaz. İleride audit gerekirse `archive` moduna geçilir ve hassas event'ler için ayrı bir önlem düşünülür.
- ⚠️ Benzer iki token tablosu ve entity var; ortak bir değişiklik iki yerde yapılmalı.
- ⚠️ Doğrulama e-postası için yeniden gönderme (resend) ve rate limit bu kapsamda yok; ihtiyaç olunca eklenecek.