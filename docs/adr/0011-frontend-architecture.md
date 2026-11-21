# ADR-011: Frontend mimarisi — Next.js BFF, token saklama ve API client'ı

- **Durum:** Kabul edildi
- **Tarih:** 2026-11-21

## Bağlam
F2 sonunda backend'in 50 endpoint'i hazır; frontend başlıyor (Next.js App Router + TypeScript, context bölüm 6). Üç karar açık kaldı (F2 raporu bölüm 11):

1. Tarayıcı backend'e nasıl ulaşacak? Backend'de CORS yapılandırması yok.
2. Token'lar nerede saklanacak? `/auth/login` ve `/auth/refresh` gövdede `{accessToken, refreshToken, expiresIn}` döner. Access 15 dk, refresh 30 gün; rotation + reuse detection var: aynı refresh token iki kez kullanılırsa kullanıcının bütün oturumları kapanır (ADR-003).
3. TypeScript client'ı `docs/api/openapi.json`'dan nasıl üretilecek?

Ayrıca frontend kodunu Claude yazacak, kullanıcı review edecek: Claude'un ortamında backend çalışmıyor, UI'ı görmek için mock'a ihtiyaç var.

## Değerlendirilen seçenekler

**Backend'e erişim ve token saklama**
1. **Tarayıcıdan doğrudan backend + CORS, token'lar `localStorage`'da:** en basit. Ama XSS ile refresh token çalınırsa saldırgan 30 gün oturum açar; her sekme kendi refresh'ini yapar, iki sekmenin eşzamanlı refresh'i reuse detection'a takılıp bütün oturumları kapatır.
2. **CORS + access token bellekte, refresh token backend'in koyduğu httpOnly cookie'de:** güvenli, ama backend'in cookie, CSRF ve CORS kurallarını öğrenmesi gerekir (ADR-003 değişir); mobil (Expo) cookie'siz çalışır, iki farklı akış oluşur.
3. **Next.js BFF (Backend for Frontend):** tarayıcı sadece Next.js'e konuşur (`/api/*` route handler'ları). Token'lar Next.js'in koyduğu `httpOnly; Secure; SameSite=Lax` cookie'lerde; route handler backend'e `Authorization: Bearer` ile iletir. Backend değişmez, mobil uygulama ileride token'ları gövdeden almaya devam eder.

**Client üretimi**
1. **openapi-typescript + openapi-fetch:** sadece tipler + ince fetch sarmalayıcı; az kod. TanStack Query hook'ları ve mock'lar elle.
2. **orval:** aynı spec'ten tipler, TanStack Query hook'ları ve **MSW mock handler'ları** üretir. Üretilen kod daha kalabalık.

## Karar
- **BFF (seçenek 3).** Tarayıcı kodu backend adresini ve token'ları hiç görmez. CORS gerekmez; backend'e dokunulmaz.
  - Refresh token sadece cookie'de; access token da cookie'de (kısa ömürlü). JavaScript ikisini de okuyamaz.
  - **Tek refresh:** refresh yalnızca BFF'te ve istek başına tek kez yapılır; backend 401 dönerse BFF bir kez refresh edip isteği tekrarlar. Eşzamanlı istekler aynı refresh sonucunu bekler (sekme yarışını reuse detection'dan korumak için). Ayrıntılar FE-3'te.
  - Public uçlar (etkinlik sayfası, misafir kaydı, e-posta link'leri) da aynı `/api` yolundan geçer; tek giriş noktası.
  - CSRF: cookie'ler `SameSite=Lax`; BFF yalnızca JSON gövdeli istekleri kabul eder (form post ile tetiklenemez).
- **orval** ile client üretimi: TanStack Query hook'ları + MSW mock'ları. Mock'lar Claude'un backend'siz ortamında ekran görüntüsü almasını ve ileride Playwright testlerini sağlar. Üretilen kod repoda, ayrı commit'te; CI, spec değişip client yeniden üretilmezse kırmızı yanar (backend'deki spec drift check'in aynası).
- **Fontlar self-host** (`@fontsource-variable`): Google Fonts'a çalışma anında istek yok (KVKK açısından da temiz), build dış ağa bağımlı değil.

## Sonuçlar
- ✅ XSS durumunda token çalınamaz; saldırgan yalnızca sayfa açıkken istek atabilir.
- ✅ Backend ve ADR-003 değişmez; mobil uygulama aynı API'yi gövde token'larıyla kullanır.
- ✅ Frontend backend'siz geliştirilebilir ve görsel olarak doğrulanabilir (MSW).
- ⚠️ Her istek bir ek atlama yapar (tarayıcı → Next.js → backend). Aynı makinede/ağda çalıştıkları için gecikme küçük; deploy'da Next.js sunucusu ayrı bir süreç (SSR için zaten gerekli).
- ⚠️ BFF katmanı güvenlik sınırının parçası olur: route handler'lar backend'in hata yanıtlarını (Problem Details) olduğu gibi iletir, kendi iç hatalarını sızdırmaz.
- ⚠️ orval sürüm yükseltmelerinde üretilen kodun diff'i büyük olabilir; ayrı commit kuralı bunu review'dan ayırır.

## Güncelleme — FE-2 (oturum uygulaması)
Uygularken iki ayrıntı netleşti; karar değişmedi, yeri değişti:
- **Yenileme `proxy.ts`'te, istekten önce:** "401 alınca yenile ve tekrarla" yerine, access cookie'si token'dan 30 sn önce düşecek şekilde yazılır (`maxAge = expiresIn - 30`). Cookie yoksa ama refresh cookie'si varsa `proxy.ts` (Next.js 16 middleware'i) isteği işlemeden önce yeniler ve yeni token'ları hem bu isteğe hem tarayıcıya yazar. Böylece Server Component'ler, Server Action'lar ve `/api` proxy'si aynı tazelenmiş token'ı görür; JWT çözmek ve isteği tekrar oynatmak gerekmez.
- **Tek refresh:** aynı refresh token için uçuştaki istek paylaşılır, bittikten sonra 15 sn aynı sonuç verilir (eski token'la gelen paralel sekmeler yeni token'ları alır). Bellek içi: tek Next.js süreci için doğru; yatay ölçeklemede paylaşılan depo (Redis) veya sticky session gerekir.
- **Giriş/çıkış Server Action:** form Next.js sunucusuna POST eder, cookie'ler sunucuda yazılır; Next.js Server Action'larda Origin kontrolü yapar (CSRF).
- Backend'e ulaşılamadığında oturum silinmez; sadece backend refresh token'ı reddederse (401) cookie'ler temizlenir.
