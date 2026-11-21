import { MapPin } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { EventStatusBadge } from "@/components/event-status-badge";
import { Notice } from "@/components/form-parts";
import { Button } from "@/components/ui/button";
import { SiteHeader } from "@/components/site-header";
import { formatEventDay, formatTimeRange } from "@/lib/format";
import { loadManagedEvent } from "../../data";
import { CancelEvent } from "./cancel-event";
import { EditEventForm } from "./edit-event-form";
import { StatusPanel } from "./status-panel";
import { TicketsSection } from "./tickets-section";

export const metadata: Metadata = { title: "Etkinliği yönet", robots: { index: false } };

type Props = { params: Promise<{ id: string }>; searchParams: Promise<{ yeni?: string }> };

function Section({ title, children }: { title: string; children: React.ReactNode }) {
    return (
        <section className="rounded-xl border bg-card p-6">
            <h2 className="mb-4 text-xl font-semibold">{title}</h2>
            {children}
        </section>
    );
}

export default async function ManageEventPage({ params, searchParams }: Props) {
    const [{ id }, { yeni }] = await Promise.all([params, searchParams]);
    const { event, tickets, questionCount } = await loadManagedEvent(id);
    const active = event.status === "DRAFT" || event.status === "PUBLISHED";
    const place = event.customAddress?.split("\n")[0];

    return (
        <>
            <SiteHeader />
            <main className="mx-auto max-w-3xl px-5 pt-6 pb-20 sm:px-8">
                <Link
                    href="/panel"
                    className="text-sm text-muted-foreground underline-offset-4 hover:underline"
                >
                    Etkinliklerim
                </Link>
                <div className="mt-2 flex flex-wrap items-center gap-3">
                    <h1 className="text-4xl font-bold tracking-[-0.02em] [font-stretch:85%] sm:text-5xl">
                        {event.title}
                    </h1>
                    <EventStatusBadge status={event.status} />
                </div>
                <p className="mt-3 text-muted-foreground nums">
                    {formatEventDay(event.startsAt ?? "")},{" "}
                    {formatTimeRange(event.startsAt ?? "", event.endsAt ?? "")}
                </p>
                {place && (
                    <p className="mt-1 flex items-center gap-2 text-muted-foreground">
                        <MapPin className="size-4 shrink-0" aria-hidden />
                        {place}
                    </p>
                )}

                {yeni && event.status === "DRAFT" && (
                    <div className="mt-6">
                        <Notice>
                            Taslak hazır. Herkese ücretsiz bir &quot;Genel Katılım&quot; bileti
                            ekledik; düzenleyebilir ya da yenilerini ekleyebilirsin.
                        </Notice>
                    </div>
                )}

                <div className="mt-8 grid gap-6">
                    <Section title="Durum">
                        <StatusPanel
                            eventId={id}
                            status={event.status}
                            slug={event.slug}
                            visibility={event.visibility}
                            cancellationReason={event.cancellationReason}
                        />
                    </Section>
                    <Section title="Biletler">
                        <TicketsSection eventId={id} tickets={tickets} editable={active} />
                    </Section>
                    <Section title="Kayıt formu">
                        <div className="flex flex-wrap items-center justify-between gap-3">
                            <p className="text-muted-foreground">
                                {questionCount === 0
                                    ? "Form yok; katılımcılar sadece bilet seçip kayıt olur."
                                    : `Kayıt olurken ${questionCount} soru soruluyor.`}
                            </p>
                            {active && (
                                <Button variant="outline" asChild>
                                    <Link href={`/panel/etkinlik/${id}/form`}>
                                        {questionCount === 0 ? "Soru ekle" : "Formu düzenle"}
                                    </Link>
                                </Button>
                            )}
                        </div>
                    </Section>
                    {active && (
                        <Section title="Etkinlik bilgileri">
                            <EditEventForm event={event} />
                            <p className="mt-4 text-sm text-muted-foreground">
                                Tarih, saat ve adres şimdilik değiştirilemiyor; gerekirse etkinliği
                                iptal edip yenisini oluştur.
                            </p>
                        </Section>
                    )}
                    {active && (
                        <Section title="İptal">
                            <CancelEvent eventId={id} />
                        </Section>
                    )}
                </div>
            </main>
        </>
    );
}
