# ADR-003: JWT access token + opaque refresh token

- **Durum:** Kabul edildi
- **Tarih:** 2026-11-17

## Bağlam
Venyu API'si stateless çalışacak. Aynı API'yi web (Next.js) ve ileride mobil uygulama (Expo) kullanacak. Uygulama tek deploy edilen bir modular monolith (ADR-001): token'ı imzalayan ve doğrulayan aynı uygulama. Kullanıcıların oturumu kapatılabilmeli (logout, çalınan token). Kimlik doğrulama her istekte DB'ye gitmemeli.

## Değerlendirilen seçenekler
1. **Server-side session (`JSESSIONID`):** Basit ve anında iptal edilebilir. Ama her istekte session lookup gerekir, mobil istemciler için cookie yönetimi zahmetlidir ve birden fazla instance'ta paylaşılan bir session store gerekir.
2. **Sadece JWT (uzun ömürlü):** Stateless, ama iptal edilemez. Çalınan bir token süresi bitene kadar geçerli kalır.
3. **Kısa ömürlü JWT access token + refresh token:** Access token stateless doğrulanır, refresh token DB'de tutulduğu için iptal edilebilir.

Alt kararlar:
- **İmza algoritması:** HS256 (simetrik, tek secret) vs RS256 (asimetrik, public key ile doğrulama). RS256'nın avantajı, token'ı başka servislerin de doğrulayabilmesi. Tek uygulama için bu gerekli değil.
- **Refresh token formatı:** JWT vs opaque (rastgele değer). Refresh token'ın asıl amacı iptal edilebilir olması, bu yüzden her durumda DB'ye bakılıyor. JWT'nin stateless avantajı burada işe yaramıyor.
- **Refresh token hash'i:** BCrypt vs SHA-256. Token yüksek entropili olduğu için brute force riski yok. BCrypt salt'lı olduğu için hash ile doğrudan lookup yapılamıyor.

## Karar
**Kısa ömürlü JWT access token + opaque refresh token** kullanıyoruz.

- **Access token:** JWT, HS256, 15 dakika. Claim'ler: `sub` (userId), `roles`, `iat`, `exp`. E-posta gibi değişebilen veya kişisel veriler token'a girmiyor. Secret `VENYU_JWT_SECRET` env değişkeninden geliyor, en az 256 bit. Encode ve decode için Spring Security'nin `NimbusJwtEncoder` / `NimbusJwtDecoder` sınıfları kullanılıyor (`spring-boot-starter-security-oauth2-resource-server`).
- **Refresh token:** 32 byte `SecureRandom`, base64url, 30 gün. DB'de (`refresh_tokens`) sadece SHA-256 hash'i tutuluyor. Ham değer yalnızca response'ta bir kez dönüyor.
- **Rotation:** Her `/auth/refresh` çağrısında eski token revoke ediliyor ve yeni bir token çifti dönüyor.
- **Reuse detection:** Revoke edilmiş bir refresh token tekrar kullanılırsa, bu çalınma işareti sayılıyor ve kullanıcının tüm refresh token'ları revoke ediliyor.
- **Eşzamanlı refresh:** Revoke işlemi koşullu ve atomik bir update ile yapılıyor (`UPDATE ... SET revoked_at = now() WHERE token_hash = ? AND revoked_at IS NULL AND expires_at > now()`). Etkilenen satır sayısı 1 ise istek kazanmış, 0 ise reddediliyor.
- **Logout:** Refresh token revoke ediliyor, response `204`. Access token için blacklist tutulmuyor, en geç 15 dakikada geçersiz oluyor.
- **Login hatası:** E-posta bulunamadığında da şifre yanlış olduğunda da `401` ve aynı mesaj dönüyor. Kullanıcı bulunamadığında da dummy hash ile şifre kontrolü yapılıyor, böylece yanıt süresinden e-postanın kayıtlı olup olmadığı anlaşılamıyor (user enumeration ve timing attack'e karşı).
- **Taşıma:** Refresh token şimdilik request body'de taşınıyor. Web için httpOnly cookie kararı F2'de, frontend ile birlikte verilecek.

## Sonuçlar
- ✅ Access token doğrulaması DB'ye gitmiyor. Her istekte sadece imza ve süre kontrol ediliyor.
- ✅ Refresh token'lar iptal edilebiliyor. Çalınan bir token tekrar kullanıldığında tespit ediliyor.
- ✅ DB sızıntısında ham refresh token'lar ele geçmiyor, sadece hash'leri.
- ⚠️ Rol değişikliği veya hesap kapatma, access token'a en geç 15 dakika sonra yansıyor.
- ⚠️ Aynı kullanıcı iki sekmeden aynı anda refresh yaparsa kaybeden istek reuse olarak algılanıyor ve kullanıcı tüm cihazlardan çıkış yapmış oluyor. Bu MVP için kabul edilebilir. Sorun olursa token family (`family_id`) yaklaşımına geçilecek.
- ⚠️ HS256'da secret'ı bilen herkes token üretebilir. Secret'ın yönetimi kritik. Başka bir servis token doğrulamak zorunda kalırsa RS256'ya geçilecek.
- ⚠️ Süresi dolmuş ve revoke edilmiş refresh token'lar tabloda birikiyor. İleride bir temizlik job'ı eklenecek (ShedLock ile).