# ADR-001: Modular monolith ile başla

- **Durum:** Kabul edildi
- **Tarih:** 2026-11-14

## Bağlam
Venyu tek geliştirici tarafından, haftada yaklaşık 10 saatle geliştiriliyor. Domain sekiz farklı sorumluluk alanı içeriyor (identity, venue, event, form, registration, payment, discovery, notification). Pilot tek semtte yapılacak, başlangıçta trafik düşük olacak. Öte yandan proje büyüdüğünde bazı alanların (örneğin payment, discovery) ayrı ölçeklenmesi gerekebilir.

## Değerlendirilen seçenekler
1. **Mikroservisler:** Her modül ayrı servis. Bağımsız deploy ve ölçekleme sağlar, ama ağ çağrıları, dağıtık transaction, servis keşfi, çoklu deploy ve gözlemlenebilirlik yükü getirir.
2. **Klasik monolith:** Tek uygulama, katman bazlı paketler (controller/service/repository). Hızlı başlar, ama modül sınırları zamanla bulanıklaşır ve sonradan ayırmak zorlaşır.
3. **Modular monolith:** Tek uygulama ve tek deploy, ama domain bazlı katı modül sınırları.

## Karar
**Modular monolith** kullanıyoruz. Modüller `com.venyu` altında domain bazlı paketler olarak tanımlanır. Sınırlar Spring Modulith ile doğrulanır (`ModularityTests`), modüller arası iletişim public API veya event üzerinden yapılır.

## Sonuçlar
- ✅ Tek deploy, tek veritabanı: operasyonel yük minimum, tek geliştirici için yönetilebilir.
- ✅ Modül sınırları testle korunduğu için bir modülü ileride servise ayırmak mümkün kalır.
- ✅ Modüller arası çağrılar process içi: ağ gecikmesi ve dağıtık transaction yok.
- ⚠️ Tüm modüller birlikte ölçeklenir. Tek bir modül darboğaz olursa ayrıştırma gerekecek.
- ⚠️ Sınır disiplini ekibe (şu an tek kişiye) bağlı. `ModularityTests` her build'de çalışmalı.
