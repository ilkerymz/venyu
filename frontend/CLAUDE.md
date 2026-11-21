# Frontend — CLAUDE.md

Kök `CLAUDE.md` genel kurallar için geçerli. Bu dosya `frontend/` içindeki çalışmaya özel.

## Çalışma şekli

- **Frontend kodunu Claude yazar, kullanıcı review edip merge eder.** (Backend'de tersi: kodu kullanıcı yazar.)
- Her PR: kısa plan (K1, K2… kararlar) → onay → `main`'den `feat/fe-<konu>` branch'i → commit'ler → lokal kontroller → PR.
- Claude `main`'e push etmez ve PR merge etmez. Force-push sadece kendi açık PR branch'ine.
- PR açıklaması şablonu: ne / neden · kararlar ve trade-off'lar · **review rehberi** (önce bakılacak dosyalar) · ekran görüntüleri · elle deneme adımları · bilerek yapılmayanlar.
- Üretilmiş kod (API client, shadcn bileşenleri) ayrı commit'te; elle yazılan kod tek başına okunabilsin.
- Frontend PR'ı backend'e dokunmaz; gerekirse ayrı küçük PR (Claude'un ortamında Maven çalışmaz, testleri kullanıcı koşar).

## Komutlar (`frontend/` içinden)

- `pnpm dev` — http://localhost:3000
- `pnpm lint`, `pnpm typecheck`, `pnpm test`, `pnpm format:check`, `pnpm build` — CI'daki sırayla aynı; PR'dan önce hepsi yeşil.
- `pnpm format` — Prettier (Tailwind class sıralaması dahil).

## Yığın

- Next.js 16 (App Router, `cacheComponents`), React 19, TypeScript, pnpm, Node 22.
- **Next.js 16 eğitim verisindeki sürümden farklı olabilir:** API'ye emin değilsen önce `node_modules/next/dist/docs/` içindeki ilgili sayfayı oku.
- Tailwind v4 (config dosyası yok, token'lar `src/app/globals.css`), shadcn/ui (Radix), Lucide.
- TanStack Query + orval (API client). Planlanan: React Hook Form + Zod, Motion, MapLibre.
- Mimari kararlar: `docs/adr/0011-frontend-architecture.md` (BFF, token saklama, client üretimi).

## API katmanı (ADR-011)

- Client `docs/api/openapi.json`'dan orval ile üretilir: `pnpm api:generate` → `src/api/generated/` (elle düzenlenmez, lint/format dışı). CI spec ile client uyumsuzsa kırmızı olur; **backend'de API değişince bu komut da çalıştırılıp çıktı commit'lenir.**
- Bütün istekler `src/api/fetcher.ts` (`apiFetch`) üzerinden: sunucuda backend'e doğrudan (`VENYU_API_URL`, varsayılan `http://localhost:8080`), tarayıcıda `/api/*` BFF proxy'sine (`app/api/[...path]/route.ts`).
- Başarısız yanıt `ApiError` fırlatır (`status`, `problem`, `messages`); 404 → `notFound()`. Kullanıcıya backend'in Türkçe `detail` / `errors` mesajları gösterilir.
- Spec'te alanlar `required` işaretli değil (springdoc + record), üretilen tiplerde her alan opsiyonel: `??` ile varsayılan ver.
- Üretilen fonksiyon adları controller metot adlarından (`get`, `get1`); import ederken anlamlı ada çevir (`get as getVenue`).

## Oturum (ADR-011, FE-2)

- Cookie'ler `venyu_at` (access, token'dan 30 sn önce düşer) ve `venyu_rt` (refresh, 30 gün); ikisi de httpOnly + SameSite=Lax (`src/auth/session.ts`).
- `src/proxy.ts` her istekten önce gerekirse yeniler (`src/auth/refresh.ts`, tek refresh) ve korunan yolları (`PROTECTED_PATHS`) girişe yönlendirir. Yetki kontrolünün asıl yeri backend.
- Kullanıcı adına sunucuda backend çağrısı: `me({ headers: await authHeaders() })` (`src/auth/server.ts`). Tarayıcıdan `/api/*` çağrılarına token'ı route handler ekler.
- Formlar Server Action + `useActionState` (`src/auth/actions.ts`); hata mesajları `describeError`. React form'u action sonrası sıfırlar: hata durumunda değerleri `state.values` ile geri doldur (şifre hariç).
- Kullanıcıya görünen yollar Türkçe (`/giris`, `/kayit`, `/hesabim`, `/sifremi-unuttum`); e-posta bağlantılarının yolları backend'den gelir (`/verify-email`, `/reset-password`, …).

## Kayıt akışı (FE-4)

- Etkinlik sayfasındaki "Katıl" paneli: oturum varsa üye kaydı (ücretsiz bilette hemen QR), yoksa misafir kaydı (30 dk e-posta onayı); `requiresAccount` ise girişe yönlendirir. Ücretli biletler F3'e kadar seçilemez.
- Dinamik form: `lib/form-visibility.ts` backend'in görünürlük kuralının (sırayla, gizli/boş = null, `ticketTypeIds`, hata → gizli) aynısı; `showIf` için npm `jexl`. Karar her zaman sunucuda. Cevap tipleri `lib/form-answers.ts`.
- Uzun/dinamik formlarda `<form action>` yerine `onSubmit` + `startTransition(dispatch)`: React başarısız gönderimden sonra formu sıfırlamaz.
- Form oluşturucu (`panel/etkinlik/[id]/form`): saf mantık `lib/form-builder.ts`. Organizatör JEXL yazmaz; koşul seçiminden `answers.<key> <op> <değer>` üretilir (sadece sembol operatörleri). Anahtarlar kalıcı `soru_N` (metinden türetilmez). Çözülemeyen koşul ham korunur. Önizleme kayıt panelinin `QuestionField`'ını kullanır.
- Kayıt paneli `components/registration-panel.tsx` ortak: etkinlik sayfasında `registerAction`, davet kabulünde (`/invitations/accept`) `acceptInvitationAction` + `guestFields="name"` (e-posta davetten gelir). Bilet hemen hazırsa (`ticketCode`) panel QR'ı gösterir.
- QR: `components/ticket-qr.tsx` (`qrcode` ile senkron SVG; sunucu ve tarayıcıda aynı).

## Tasarım sistemi

- Vitrin: `/tasarim` (noindex). Yeni temel bileşen eklenince oraya da eklenir.
- Renkler İznik çinisinden: `gece` (metin), `kagit` (zemin), `cini` (eylem), `turkuaz` (olumlu durum), `nar` (kıtlık / hata), `kehribar` (uyarı, `warning`), `sis` (ikincil metin).
- **Bileşenlerde ham renk (`#2d3fd3`, `blue-600`) yok;** semantik token: `primary`, `success`, `scarce`, `destructive`, `muted-foreground`…
- Başlıklar `font-display` (Bricolage Grotesque), arayüz metni `font-sans` (Onest). Fontlar `@fontsource-variable` ile self-host (dış istek yok).
- Saat, fiyat, sayaç: `nums` utility'si (tabular rakam).
- İmza bileşenler: `EventTicket` (bilet koçanı), `ThresholdMeter` (minimum katılımcı eşiği, kişi başı kutu).
- Metin: Türkçe, sade, kullanıcının diliyle. Buton ne yapacağını söyler ("Etkinliği yayınla", "Gönder" değil). Hata mesajı ne olduğunu ve nasıl düzeltileceğini söyler.
- Kalite tabanı: mobil uyumlu, görünür klavye odağı, `prefers-reduced-motion`, erişilebilir etiketler (`aria-*`, `sr-only`).

## Kod yerleşimi

```
src/
├── app/              (route'lar; sayfa dosyaları ince)
├── components/
│   ├── ui/           (shadcn kaynakları — sadece gerektiğinde düzenlenir)
│   └── *.tsx         (Venyu bileşenleri, kebab-case dosya adı)
├── api/              (fetcher, ApiError, proxy yardımcıları; generated/ = orval çıktısı)
└── lib/              (saf yardımcılar: format, utils)
```

- Route'a özel veri yükleme route klasöründe (`app/e/[slug]/data.ts`); her veri sayfasında `loading.tsx`, `error.tsx`, gerekiyorsa `not-found.tsx`.
- **Instant navigation (Next.js 16 dev "Insight" uyarıları sıfır kalmalı):** URL'e (`searchParams`, `params`), cookie'ye ya da önbelleksiz veriye bağlı kısım ya aynı segmentteki `loading.tsx` ile ya da sayfa içinde `<Suspense>` ile sarılır; statik kısım (başlık) dışarıda kalır. `connection()` ile sayfayı dinamik yapma; tarayıcıya özgü değer (bugünün tarihi) istemcide `useSyncExternalStore` ile.
- Next.js 16'da `error.tsx` prop'u `retry` (`reset` değil); `notFound()`'u yakalayan `try/catch`'te `unstable_rethrow`.
- **Hesabım (`/hesabim`, FE-11) kullanıcının merkezi:** solda profil kartı (e-posta ve telefon doğrulama durumu, doğrulanmamışın yanında "Doğrula"), sağda organizatörse "Etkinliklerim" kartları (`components/event-card.tsx`, yaklaşan / geçmiş `lib/datetime.ts` `splitByTime`) + "Etkinlik oluştur" + mekan rezervasyonları, değilse "Organizatör ol". `/panel` sadece Hesabım'a yönlenir. Herkeste "Katıldığım etkinlikler" (FE-12): `/me/registrations` etkinlik bilgisiyle gelir; görünürlük ve kart durumu `lib/registrations.ts` (kullanıcının iptal ettiği / süresi dolan gizli, etkinlik başına bir kart, etkinlik iptalse "Etkinlik iptal edildi"). Kart herkese açık etkinlikte `/e/<slug>`'a, değilse (sadece davetli, iptal) `/hesabim/bilet/<id>`'ye gider (etkinlik sayfası katılımcıya 404). Doğrulanmamış e-postada "Doğrula" `POST /me/email-verification` (dakikada bir). Aynı sayfadaki iki "Doğrula" düğmesinin erişilebilir adları ayrı ("E-postayı doğrula", "Telefonu doğrula").
- Kart etkinlik sayfasına (`/e/<slug>`) gider; etkinlik oturumdaki organizatörünse sayfanın üstünde `OrganizerBar` (durum, taslaksa "Etkinliği yayınla", yönetim sekmelerine kısayollar). Sahiplik `/me/events` içinde slug ile bulunur (public yanıtta organizatör yok); oturum varsa `by-slug` token'la çağrılır, organizatör taslağını da görür.
- Organizatör paneli `app/panel/` altında: veri `panel/data.ts` (401 → giriş, 403 → hesabım, 404 → notFound), işlemler `panel/actions.ts` (Server Action + `refresh()`); id'li action'lar `.bind(null, id)` ile.
- Etkinlik yönetimi `panel/etkinlik/[id]/layout.tsx`: ortak başlık (Suspense içinde, `loadEventSummary` React `cache` ile istek başına tek) + sekmeler (Genel, Katılımcılar, Davetler, Kayıt formu, Kapı). Alt sayfalar sadece içerik; her birinin `loading.tsx`'i var.
- Kapı modu: `qr-scanner` (worker'lı, iOS'ta da çalışır) dinamik import; aynı kod 4 sn içinde tekrar okunursa yok sayılır; elle kod girişi her zaman var. Kamera HTTPS ister (localhost hariç).
- CSV: UTF-8 BOM + `;` (Excel TR), formül enjeksiyonuna karşı `=+-@` ile başlayan hücrelere `'`.
- Mekan sahibi paneli `app/mekan/` (korunan yol): mekan listesi `/me` id'si ile `GET /venues` süzülerek; `[id]` layout + sekmeler (Takvim, Rezervasyonlar, Bilgiler), başkasının mekanı 404. Sekmeler ortak `components/section-tabs.tsx`. Sayfa verisinde 401/403/404 kuralı `api/guard.ts`.
- Organizatörün mekan tarafı: herkese açık `/mekanlar` (semt filtresi) ve `/mekanlar/[id]` (boş saatler, organizatörse "Bu saati ayır" → 24 saatlik hold). Hesabım'da "Mekan rezervasyonların" (`loadMyBookings`: mekan + slot + bağlı etkinlik); onaylı ve kullanılmamış rezervasyon yeni etkinlik formunda "Venyu mekanında" seçeneği olur (`?rezervasyon=<id>`), tarih/saat/yer rezervasyondan.
- Render sırasında `Date.now()`: bileşen içinde değil (lint), veri fonksiyonunda ve önce cookie/params gibi dinamik bir erişim ya da `connection()` (prerender hatası).
- Varsayılan Server Component; `"use client"` sadece etkileşim gereken yaprak bileşende.
- Zaman: backend UTC döner, ekranda her zaman `Europe/Istanbul` (`lib/format.ts`). Para: `formatPrice(amount, currency)`.
- shadcn bileşeni eklemek: `pnpm dlx shadcn@latest add <ad>`. Claude'un ortamında shadcn registry'si kapalı; orada kaynak `github.com/shadcn-ui/ui` → `apps/v4/registry/new-york-v4/ui/` altından kopyalanır, `from "cn"` → `from "@/lib/utils"`.

## Test

- Vitest; test dosyası test ettiği dosyanın yanında (`format.ts` → `format.test.ts`).
- Saf mantık (biçimleme, form koşulları, hata eşleme) unit test'le; UI akışları ileride Playwright + MSW mock'larıyla.
