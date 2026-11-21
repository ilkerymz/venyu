# ADR-009: Dinamik kayıt formu (form v1)

- **Durum:** Kabul edildi
- **Tarih:** 2026-11-21

## Bağlam
Organizatör etkinliğe opsiyonel bir kayıt formu ekleyebilmeli: koşullu sorular ("araçla geliyorsan plakan?") ve bilet türüne göre sorular. Fiyat etkisi, otomatik onay ve seçenek kontenjanı v2'de (context bölüm 7). Form hem frontend'de (göster/gizle) hem sunucuda (doğrulama) aynı kurallarla çalışmalı. Kurallar kullanıcıdan gelen veri olduğu için sunucuda çalıştırılması bir güvenlik konusu.

## Değerlendirilen seçenekler

**Şemanın saklanması**
1. İlişkisel tablolar (`form_fields`, `field_options`): alan bazında sorgu yapılabilir, ama versiyonlama her tabloyu kopyalamayı gerektirir.
2. **Tek JSONB doküman:** şema hep bütün olarak okunur; versiyon = yeni satır.

**Form değişince**
1. İlk kayıttan sonra formu kilitle: basit ama organizatör yazım hatasını bile düzeltemez.
2. **Her kaydetme yeni, değişmez versiyon:** kayıt hangi versiyonla doldurulduğunu tutar.

**Kural dili**
1. Kendi küçük DSL'imiz / JSON koşul ağacı: güvenli ama her operatörü biz yazarız, frontend için ayrıca yorumlayıcı gerekir.
2. SpEL: Spring'in parçası, ama tip/metot erişimi geniş; sandbox'lamak zor.
3. **JEXL (commons-jexl3):** feature ve permission bazında kısıtlanabilir; aynı ifadeler tarayıcıda JS JEXL ile değerlendirilebilir; geliştirici iş yerinde de JEXL kullanıyor.

**Modül sınırı**
1. Form event modülünün içinde.
2. **Ayrı form modülü:** şema ve kurallar form'da, cevaplar registration'da; registration form'un public API'sini çağırır (registration → form → event).

## Karar
- **Saklama:** `form_schemas(event_id, version, definition JSONB, created_by, created_at)`, `UNIQUE (event_id, version)`. Satırlar değişmez; eşzamanlı iki kaydetme aynı versiyonu üretirse unique ihlali → 409. Boş alan listesi = formu kaldırmak. JSONB kolonu entity'de `String` (`columnDefinition = "jsonb"`, `@ColumnTransformer(write = "?::jsonb")`), dönüşüm uygulamada Jackson 3 ile.
- **Alan modeli:** `key` (`[a-z][a-z0-9_]{0,39}`, formda tekil), `label`, `type` (`TEXT`, `LONG_TEXT`, `NUMBER`, `BOOLEAN`, `SINGLE_CHOICE`, `MULTI_CHOICE`), `required`, `options`, `showIf`, `ticketTypeIds` (boş = bütün biletler). En fazla 50 alan. Zorunlu `BOOLEAN` true olmalı (onay kutusu).
- **JEXL sandbox:** `JexlPermissions.RESTRICTED`; `JexlFeatures` ile yan etki, döngü, lambda, `new`, metot/fonksiyon çağrısı, yerel değişken, pragma ve annotation kapalı (döngü olmadığı için değerlendirme süresi sınırlı, timeout gerekmez); `strict`, parse cache. İfade **kaydederken** doğrulanır: söz dizimi + sadece `answers.<önceki alanın key'i>` ve `ticketTypeId` kullanılabilir. "Sadece önceki alanlar" kuralı döngüsel bağımlılığı imkânsız kılar ve formun tek geçişte değerlendirilmesini sağlar. Değerlendirme hatası (örn. boş sayı alanıyla karşılaştırma) "gizli" sayılır. İfadeyi organizatör yazmaz, frontend'in form builder'ı üretir.
- **Sunucu istemciye güvenmez:** `EventForms.validate(eventId, ticketTypeId, answers)` görünürlüğü yeniden hesaplar, gizli alanların cevaplarını atar, görünen zorunlu alanları ve tipleri kontrol eder, bilinmeyen key'leri reddeder. Sonuç sealed `FormValidation` (`Valid(formVersion, answers) | Invalid(errors)`). Formu olmayan etkinlikte cevap gönderilemez.
- **Okuma:** `GET /events/{id}/form` public; organizatör her zaman, diğerleri sadece herkese açık etkinlikte (yoksa 404). INVITE_ONLY etkinlikte form davet önizlemesinde döner. Yazma `PUT /events/{id}/form` (organizatör, etkinlik başlamadan önce); şema hataları tek yanıtta `errors` listesiyle (400).
- **Cevaplar:** registration modülünde `registrations.answers JSONB` + `form_version`.

## Sonuçlar
- ✅ Kurallar veri olarak saklanıyor; yeni koşul için deploy gerekmiyor.
- ✅ Eski kayıtlar doldurdukları versiyonla birlikte okunabilir.
- ✅ Kötü niyetli ifade (dosya, ağ, reflection, sonsuz döngü) kaydedilemiyor; kaydedilen ifade sadece cevapları okuyabiliyor.
- ⚠️ Java JEXL ile JS JEXL'in davranışı birebir aynı değil; frontend sadece göster/gizle için kullanır, karar her zaman sunucuda.
- ⚠️ JSONB içindeki alanlar üzerinde DB seviyesinde kısıt yok; tutarlılık uygulamada.
- ⚠️ commons-jexl3 3.6.1'e sabitlendi; 3.7 varsayılan davranışları değiştirdiği için yükseltme ayrı bir iş (sandbox testleriyle birlikte).
- ⚠️ Form değişince eski versiyonla açılmış ama henüz gönderilmemiş bir sayfa yeni versiyona göre doğrulanır; frontend 400 alınca formu yeniden çeker.
