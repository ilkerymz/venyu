import type { Metadata } from "next";
import { EventTicket } from "@/components/event-ticket";
import { ThresholdMeter } from "@/components/threshold-meter";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Wordmark } from "@/components/wordmark";

/**
 * Geliştirme için tasarım sistemi vitrini: token'lar ve temel bileşenler
 * tek sayfada. Ürün sayfası değil; arama motorlarına kapalı.
 */
export const metadata: Metadata = {
    title: "Tasarım sistemi",
    robots: { index: false, follow: false },
};

const colors = [
    { name: "Gece", token: "--gece", hex: "#15172E", use: "Metin" },
    { name: "Kâğıt", token: "--kagit", hex: "#F3F4F8", use: "Sayfa zemini" },
    { name: "Çini", token: "--cini", hex: "#2D3FD3", use: "Eylem, bağlantı, odak" },
    { name: "Turkuaz", token: "--turkuaz", hex: "#0B8F80", use: "Olumlu durum, eşik doldu" },
    { name: "Nar", token: "--nar", hex: "#D7264E", use: "Kıtlık, hata, iptal" },
    { name: "Kehribar", token: "--kehribar", hex: "#B45C00", use: "Uyarı, zaten girdi" },
    { name: "Sis", token: "--sis", hex: "#5E6280", use: "İkincil metin" },
];

const typeScale = [
    {
        className: "font-display text-6xl font-bold tracking-[-0.03em] [font-stretch:82%]",
        label: "Display 60",
        sample: "Akustik gece",
    },
    {
        className: "font-display text-4xl font-bold tracking-tight",
        label: "Başlık 36",
        sample: "Bilet türleri",
    },
    { className: "font-display text-2xl font-semibold", label: "Başlık 24", sample: "Kayıt formu" },
    { className: "text-lg", label: "Gövde 18", sample: "Etkinlik 15 kişiye ulaşınca kesinleşir." },
    {
        className: "text-base",
        label: "Gövde 16",
        sample: "Mekan onayladığında sana haber vereceğiz.",
    },
    {
        className: "text-sm text-muted-foreground",
        label: "Küçük 14",
        sample: "Çarşamba, 19:00–21:30",
    },
];

function Section({ title, children }: { title: string; children: React.ReactNode }) {
    return (
        <section className="border-t py-10">
            <h2 className="mb-6 text-xl font-semibold">{title}</h2>
            {children}
        </section>
    );
}

export default function DesignSystemPage() {
    return (
        <main className="mx-auto max-w-5xl px-5 py-10 sm:px-8">
            <Wordmark />
            <h1 className="mt-8 text-4xl font-bold tracking-tight">Tasarım sistemi</h1>
            <p className="mt-3 max-w-2xl text-muted-foreground">
                Renkler İznik çinisinden gelir. Başlıklar Bricolage Grotesque, arayüz metni Onest.
                Bileşenler ham renk değil semantik token kullanır (<code>primary</code>,{" "}
                <code>success</code>, <code>scarce</code>).
            </p>

            <Section title="Renkler">
                <ul className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                    {colors.map((c) => (
                        <li key={c.token} className="overflow-hidden rounded-lg border bg-card">
                            <div
                                className="h-20 border-b"
                                style={{ background: `var(${c.token})` }}
                            />
                            <div className="p-3 text-sm">
                                <p className="font-semibold">{c.name}</p>
                                <p className="text-muted-foreground nums">{c.hex}</p>
                                <p className="mt-1 text-muted-foreground">{c.use}</p>
                            </div>
                        </li>
                    ))}
                </ul>
            </Section>

            <Section title="Tipografi">
                <dl className="space-y-5">
                    {typeScale.map((t) => (
                        <div
                            key={t.label}
                            className="grid gap-1 sm:grid-cols-[8rem_1fr] sm:items-baseline"
                        >
                            <dt className="text-sm text-muted-foreground">{t.label}</dt>
                            <dd className={t.className}>{t.sample}</dd>
                        </div>
                    ))}
                </dl>
            </Section>

            <Section title="Butonlar">
                <div className="flex flex-wrap items-center gap-3">
                    <Button>Etkinliği yayınla</Button>
                    <Button variant="secondary">Taslağı kaydet</Button>
                    <Button variant="outline">Önizle</Button>
                    <Button variant="ghost">Vazgeç</Button>
                    <Button variant="destructive">Etkinliği iptal et</Button>
                    <Button disabled>Satışta değil</Button>
                </div>
            </Section>

            <Section title="Form alanları">
                <div className="grid max-w-md gap-5">
                    <div className="grid gap-2">
                        <Label htmlFor="name">Ad soyad</Label>
                        <Input id="name" placeholder="Ayşe Yılmaz" />
                    </div>
                    <div className="grid gap-2">
                        <Label htmlFor="email">E-posta</Label>
                        <Input
                            id="email"
                            type="email"
                            defaultValue="ayse@"
                            aria-invalid
                            aria-describedby="email-error"
                        />
                        <p id="email-error" className="text-sm text-destructive">
                            Geçerli bir e-posta adresi yaz, örneğin ayse@ornek.com.
                        </p>
                    </div>
                </div>
            </Section>

            <Section title="Durum etiketleri">
                <div className="flex flex-wrap gap-2">
                    <Badge>Yayında</Badge>
                    <Badge variant="secondary">Taslak</Badge>
                    <Badge className="bg-success-soft text-success">Kesinleşti</Badge>
                    <Badge className="bg-scarce-soft text-scarce">Son 3 bilet</Badge>
                    <Badge variant="outline">Davetli</Badge>
                    <Badge variant="destructive">İptal edildi</Badge>
                </div>
            </Section>

            <Section title="Eşik göstergesi">
                <div className="grid max-w-md gap-8">
                    <ThresholdMeter minimum={15} current={3} />
                    <ThresholdMeter minimum={15} current={15} />
                    <ThresholdMeter minimum={120} current={47} />
                </div>
            </Section>

            <Section title="Bilet">
                <div className="grid max-w-xl gap-6">
                    <EventTicket
                        title="Kitap kulübü: Kürk Mantolu Madonna"
                        place="Defne Kitabevi, Kadıköy"
                        startsAt="2026-12-09T16:00:00Z"
                        endsAt="2026-12-09T18:00:00Z"
                        price={0}
                        currency="TRY"
                    />
                </div>
            </Section>
        </main>
    );
}
