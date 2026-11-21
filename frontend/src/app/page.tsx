import { EventTicket } from "@/components/event-ticket";
import { Button } from "@/components/ui/button";
import { SiteHeader } from "@/components/site-header";
import { Wordmark } from "@/components/wordmark";

const steps = [
    {
        title: "Mekan boş saatini açar",
        body: "Kafe, atölye ya da bar; hafta içi akşamları gibi boş kalan saatlerini takvime ekler.",
    },
    {
        title: "Organizatör etkinliği kurar",
        body: "Saati ayırır, bilet türlerini ve kayıt formunu tanımlar. Mekan onaylayınca etkinlik yayına çıkar.",
    },
    {
        title: "Katılımcılar yerini ayırır",
        body: "Herkese açık etkinliği bulur ya da davetle gelir; biletini telefonunda QR olarak taşır.",
    },
    {
        title: "Eşik dolunca etkinlik kesinleşir",
        body: "Yeterli katılımcı toplanmazsa kartlardan çekim yapılmaz. Kimse boş salona para ödemez.",
    },
];

export default function HomePage() {
    return (
        <>
            <SiteHeader>
                <Button variant="ghost" asChild className="hidden sm:inline-flex">
                    <a href="#nasil-calisir">Nasıl çalışır</a>
                </Button>
            </SiteHeader>

            <main>
                <section className="mx-auto grid max-w-6xl items-center gap-14 px-5 pt-10 pb-20 sm:px-8 lg:grid-cols-[1.1fr_1fr] lg:gap-10 lg:pt-16 lg:pb-28">
                    <div className="max-w-xl">
                        <h1 className="text-[2.75rem] leading-[0.95] font-bold tracking-[-0.025em] [font-stretch:82%] sm:text-7xl">
                            Mekanı bul, bileti sat, tek akışta.
                        </h1>
                        <p className="mt-6 text-lg leading-relaxed text-muted-foreground">
                            Venyu, mekanların boş akşamlarını etkinlik kuran insanlarla buluşturur.
                            Saat, bilet, kayıt formu ve kapıdaki giriş aynı yerde.
                        </p>
                        <div className="mt-9 flex flex-wrap items-center gap-3">
                            <Button size="lg" asChild>
                                <a href="#nasil-calisir">Nasıl çalıştığını gör</a>
                            </Button>
                            <p className="text-sm text-muted-foreground">
                                Pilot aşamasında, tek semtte.
                            </p>
                        </div>
                    </div>

                    <div className="relative mx-auto w-full max-w-md lg:max-w-none">
                        <EventTicket
                            title="Seramik atölyesi: ilk kupan"
                            place="Kafe Lale, Moda"
                            startsAt="2026-12-02T16:00:00Z"
                            endsAt="2026-12-02T18:30:00Z"
                            price={450}
                            currency="TRY"
                            threshold={{ minimum: 15, current: 11 }}
                        />
                        <EventTicket
                            className="mt-5 lg:mt-6 lg:ml-12"
                            title="Akustik gece"
                            place="Arka Bahçe, Yeldeğirmeni"
                            startsAt="2026-12-05T17:30:00Z"
                            endsAt="2026-12-05T20:00:00Z"
                            price={200}
                            currency="TRY"
                            threshold={{ minimum: 20, current: 24 }}
                        />
                    </div>
                </section>

                <section id="nasil-calisir" className="scroll-mt-4 border-y bg-card">
                    <div className="mx-auto max-w-6xl px-5 py-20 sm:px-8 lg:py-24">
                        <h2 className="max-w-lg text-3xl leading-tight font-bold tracking-tight sm:text-4xl">
                            Bir etkinlik Venyu&apos;da nasıl oluşur
                        </h2>
                        <ol className="mt-12 grid gap-10 sm:grid-cols-2 lg:grid-cols-4 lg:gap-8">
                            {steps.map((step, i) => (
                                <li key={step.title} className="border-t-2 border-primary pt-5">
                                    <span className="font-display text-sm font-semibold text-primary nums">
                                        {i + 1}
                                    </span>
                                    <h3 className="mt-2 text-lg leading-snug font-semibold">
                                        {step.title}
                                    </h3>
                                    <p className="mt-2 leading-relaxed text-muted-foreground">
                                        {step.body}
                                    </p>
                                </li>
                            ))}
                        </ol>
                    </div>
                </section>
            </main>

            <footer className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-5 py-10 text-sm text-muted-foreground sm:px-8">
                <Wordmark className="text-xl text-foreground" />
                <p>© 2026 Venyu</p>
            </footer>
        </>
    );
}
