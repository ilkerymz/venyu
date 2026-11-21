# ADR-010: Bilet kodu, check-in ve misafirin "yönet" link'i

- **Durum:** Kabul edildi
- **Tarih:** 2026-11-21

## Bağlam
F2'nin son halkası: onaylanan katılımcı kapıda bir QR gösterir, organizatör okutur, aynı bilet ikinci kez geçmez. Organizatör kimlerin kayıtlı olduğunu (ad, e-posta, form cevapları) ve kimin girdiğini görür. Hesabı olmayan misafirin biletini görebileceği ve kaydını iptal edebileceği bir yol olmalı. MVP'de check-in online (v2: offline).

## Değerlendirilen seçenekler

**QR'ın içeriği**
1. **Rastgele, tahmin edilemez kod; DB'de düz:** sorgu ile doğrulanır. Secret yok. İptal edilen kaydın kodu durum kontrolüyle kendiliğinden geçersiz.
2. HMAC imzalı payload (`registrationId.imza`): kod saklanmaz, offline doğrulamaya hazır. Ama secret yönetimi gerekir; secret sızarsa her bilet sahtelenebilir; iptal yine DB'ye bakmayı gerektirir.
3. Kodu hash'leyip saklamak: katılımcı biletini tekrar göremez (QR'ı yeniden çizmek için ham kod gerekir).

**QR görseli**
1. Backend PNG üretir (ZXing).
2. **Backend sadece kodu döner, frontend çizer;** e-postadaki görseli gerçek sağlayıcı geldiğinde notification üretir.

**Çift okutma**
1. Entity + `@Version`: ikinci okutma optimistic hata alır, ama önce okuma + sonra yazma iki gidiş.
2. **Tek koşullu UPDATE** (`... WHERE event_id = ? AND ticket_code = ? AND status = 'CONFIRMED'`): PostgreSQL READ COMMITTED'da bekleyen ikinci UPDATE koşulu güncel satırda yeniden değerlendirir, 0 satır günceller.

**Misafirin kaydına erişimi**
1. Bilet kodunu yönetim için de kullanmak: kodu kapıdaki görevli görür, iptal yetkisi vermemeli.
2. **Ayrı "yönet" token'ı** (32 byte, hash'li), sadece bilet e-postasında.

## Karar
- **Bilet kodu:** kayıt CONFIRMED olduğu anda (üye, e-postasını onaylayan misafir, davetli üye/misafir) 16 byte rastgele kod (base64url, 22 karakter; QR küçük kalsın). `registrations.ticket_code` unique; `CHECK`: CONFIRMED/CHECKED_IN'de dolu. Domain'de `TicketCredentials` factory'lere geçer, "kodsuz onaylı kayıt" ara durumu yok. Mevcut kayıtlara migration'da `gen_random_uuid()` tabanlı kod.
- **Check-in:** `POST /events/{id}/check-ins {ticketCode}` (ORGANIZER + sahiplik). Kapı penceresi: başlangıçtan 3 saat önce → bitişe kadar. Tek native koşullu UPDATE (`status = CHECKED_IN`, `checked_in_at`, `checked_in_by`, `version + 1`); ardından okuma ile sonuç/neden: başka etkinliğin veya olmayan kod 404, zaten girilmiş 409 + `checkedInAt`, iptal 409. `checked_in_*` alanları entity'de `insertable/updatable = false`, sadece bu sorgu yazar. `CHECK ((status = 'CHECKED_IN') = (checked_in_at IS NOT NULL))`.
- **Katılımcı listesi:** `GET /events/{id}/registrations` (ORGANIZER + sahiplik), EXPIRED hariç. Üyenin ad/e-postası yeni identity API'si `UserDirectory.findContacts(ids)` ile (projeksiyon, tek sorgu); misafirinki kayıttan. Form cevapları dahil, bilet kodu hariç. Organizatör katılımcının e-postasını görür, telefonunu görmez.
- **Misafir:** bilet hazır olunca `TicketIssued` (registration kök paketi) → notification bilet kodu + "yönet" link'i gönderir. `POST /registrations/manage` (görüntüle) ve `/manage/cancel` (iptal, koltuk bırakılır); token body'de, tek kullanımlık değil. Üye biletini `/me/registrations`'ta (`ticketCode`) görür, kendi hesabıyla iptal eder; üyeye bilet e-postası şimdilik yok.
- **Sahiplik kontrolü** registration içinde tek bileşende (`OrganizerEvents`): davet, liste, check-in.

## Sonuçlar
- ✅ Aynı bilet kaç kapıda aynı anda okutulursa okutulsun tek giriş (20 eşzamanlı okutma testi).
- ✅ Secret yok; iptal edilen bilet ek işlem gerektirmeden geçersiz.
- ✅ Kapıdaki görevli kodu görse de kaydı yönetemez.
- ⚠️ Kodlar DB'de düz: DB sızıntısında biletler kopyalanabilir (yine de tek giriş kuralı geçerli). Offline check-in (v2) imzalı formata geçişi gerektirir.
- ⚠️ Check-in sadece organizatör; kapı görevlisi rolü, check-in'i geri alma ve CSV export yok.
- ⚠️ Katılımcı listesinde pagination yok (pilot ölçeğinde yüzlerce kayıt).
- ⚠️ Check-in sorgusu native: kolon adı değişikliği derlemede değil testte yakalanır.
- ⚠️ "Yönet" token'ı süresiz; e-posta ele geçirilirse kayıt iptal edilebilir (etkinlik bitince anlamsızlaşır).
