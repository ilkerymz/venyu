# ADR-002: Veritabanı olarak PostgreSQL

- **Durum:** Kabul edildi
- **Tarih:** 2026-11-14

## Bağlam
Venyu'nun veri ihtiyaçları birbirinden farklı üç tür içeriyor:
- **İlişkisel ve tutarlılık kritik veri:** bilet satışı, koltuk kapasitesi, ödeme ve double-entry ledger. Atomik işlemler, constraint'ler ve güçlü transaction garantisi şart.
- **Yarı yapılandırılmış veri:** dinamik form şemaları ve cevapları. Her etkinliğin formu farklı; cevaplar sabit bir tablo yapısına sığmıyor ama yine de sorgulanabilir olmalı.
- **Konum verisi:** feed'de "yakınımdaki etkinlikler" araması (MVP sonrası).

Ayrıca proje tek geliştiriciyle ve düşük bütçeyle yürüyor. Lisans maliyeti olmamalı, lokal geliştirme ve testler (Testcontainers) kolay olmalı.

## Değerlendirilen seçenekler
1. **PostgreSQL:** Açık kaynak, güçlü ACID garantisi, `JSONB` ile indekslenebilir yarı yapılandırılmış veri, PostGIS ile konum sorguları.
2. **MySQL:** Açık kaynak ve yaygın. JSON desteği var ama indeksleme ve sorgu yetenekleri PostgreSQL'in `JSONB`'si kadar güçlü değil. Coğrafi özellikler PostGIS'e göre sınırlı.
3. **Oracle:** Geliştiricinin iş deneyimi var ve kurumsal olarak çok güçlü. Ama lisans maliyeti yüksek, lokal geliştirme ve container kullanımı daha zahmetli, küçük bir girişim için fazla ağır.
4. **MongoDB:** Dinamik formlar için doğal bir uyum sağlar. Ama ödeme, ledger ve kapasite gibi tutarlılığın kritik olduğu kısımlarda ilişkisel bir modelin constraint ve transaction gücünü kaybederiz. İki ayrı veritabanı işletmek de tek geliştirici için fazla yük.

## Karar
Tüm modüller için tek veritabanı olarak **PostgreSQL** kullanıyoruz. Lokal geliştirmede ve testlerde `postgres:17` imajı kullanılır. Şema Liquibase ile yönetilir. Dinamik form verisi `JSONB` kolonlarında tutulur, konum araması ihtiyaç doğduğunda PostGIS ile eklenir.

## Sonuçlar
- ✅ Tutarlılık kritik veri (ödeme, kapasite) ve esnek veri (formlar) tek veritabanında, tek transaction içinde yönetilebilir.
- ✅ Lisans maliyeti yok. Docker ve Testcontainers ile lokal ortam ve CI kolay.
- ✅ PostGIS sayesinde konum özellikleri için ayrı bir sistem gerekmeyecek.
- ⚠️ Oracle deneyimi birebir aktarılamaz: SQL lehçesi, sequence/identity davranışları ve bazı fonksiyonlar farklı.
- ⚠️ `JSONB` sorguları PostgreSQL'e özgü. Veritabanı değiştirmek ileride daha maliyetli olur.
- ⚠️ Modüller aynı veritabanını paylaşıyor. Modül sınırları veritabanı seviyesinde değil, kod seviyesinde korunuyor (ADR-001).
