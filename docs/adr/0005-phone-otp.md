# ADR-005: Telefon doğrulama OTP'si: kod saklama ve rate limiting

- **Durum:** Kabul edildi
- **Tarih:** 2026-11-21

## Bağlam
Ücretli işlemler için telefon doğrulaması gerekiyor (seviye 2). Kullanıcıya SMS ile 6 haneli bir kod gönderilir. Gereksinimler: kullanıcı başına 10 dakikada en fazla 3 kod isteği, kod başına en fazla 5 deneme. Tablo (`phone_otp_challenges`) 4a'da oluşturuldu.

Bu akış önceki token'lardan iki noktada ayrılıyor: kodun olasılık uzayı çok küçük (10⁶) ve "say, sınır altındaysa ekle" kuralı iki ayrı işlemden oluşuyor.

## Değerlendirilen seçenekler

**Kod saklama**
1. **SHA-256 (ADR-003/004'teki gibi):** Token'lar için güvenli, çünkü olasılık uzayı 2²⁵⁶. 6 haneli kodda ise DB sızarsa 10⁶ olasılığın hepsi bir saniyenin altında hesaplanır.
2. **BCrypt (mevcut `PasswordEncoder`):** Bilerek yavaş. 10⁶ olasılığı denemek saatler sürer, kodun ömrü 5 dakika. Kod hash ile aranmadığı (kullanıcının aktif challenge'ı id ile bulunduğu) için salt sorun değil.

**İstek sınırı (10 dakikada 3)**
1. **Say, sonra ekle (kilitsiz):** Eşzamanlı istekler aynı sayıyı görür ve hepsi ekler. Sınır delinir (check-then-act yarışı).
2. **Kullanıcı satırını kilitle (`SELECT ... FOR UPDATE`):** Aynı kullanıcının istekleri sıraya girer, farklı kullanıcılar etkilenmez. Ek altyapı gerektirmez.
3. **Harici rate limiter (Redis vb.):** Çok instance'lı ve yüksek trafikli ortam için uygun, ama şu an gereksiz bir bağımlılık.

**Deneme sınırı (kod başına 5)**
- Deneme sayısı koşullu ve atomik bir update ile artırılır: `attempts = attempts + 1 WHERE attempts < 5 AND consumed_at IS NULL AND expires_at > now`. Etkilenen satır 0 ise deneme reddedilir.

## Karar
- Kod: 6 hane, `SecureRandom`, 5 dakika geçerli. DB'de **BCrypt** hash'i tutulur.
- İstek sınırı: **kullanıcı satırı `PESSIMISTIC_WRITE` ile kilitlenir**, ardından son 10 dakikadaki challenge'lar sayılır ve sınır altındaysa yenisi eklenir. Aşılırsa `429`.
- Yeni istek, kullanıcının kullanılmamış eski challenge'larını geçersiz kılar (sadece son kod geçerlidir).
- Deneme sınırı: atomik `registerAttempt` update'i. Yanlış kodda fırlatılan exception deneme sayacını geri almasın diye `@Transactional(noRollbackFor = InvalidOtpCodeException.class)` (ADR-003'teki rollback tuzağının aynısı).
- Başka bir kullanıcıda doğrulanmış numara `409`. Son garanti DB'deki `users_verified_phone_uq` partial unique index'i.
- Endpoint'ler kimlik doğrulama gerektirdiği için `/auth/**` altında değil, `/me/phone/verification` altında.
- Kod, `PhoneVerificationRequested` event'i ile notification modülüne taşınır (ADR-004'teki trade-off'un aynısı, ömür 5 dakika). Geliştirmede log'a yazılır.

## Sonuçlar
- ✅ DB sızsa bile kodlar ömürleri içinde çözülemez.
- ✅ Rate limit eşzamanlı isteklerde de kesin olarak uygulanır, ek altyapı yok.
- ✅ Brute force 5 denemeyle sınırlı; 10 dakikada en fazla 3 kod × 5 deneme = 15 tahmin (1 milyonda 15).
- ⚠️ BCrypt her doğrulamada ~100 ms CPU harcar. 5 deneme sınırıyla kabul edilebilir.
- ⚠️ Satır kilidi, aynı kullanıcının eşzamanlı isteklerini kısa süreliğine bekletir. Tek instance ve bu trafikte sorun değil. Çok instance'a geçildiğinde de çalışır (kilit DB'de), ama yüksek trafikte harici bir rate limiter yeniden değerlendirilir.
- ⚠️ "Bu numara başka hesapta kayıtlı" yanıtı numaranın kayıtlı olduğunu açığa vurur. Rate limit bu sorgulamayı da sınırlıyor; UX için kabul edildi.