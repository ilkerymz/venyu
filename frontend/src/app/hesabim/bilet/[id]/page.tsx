import { MapPin } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { MyTicket } from "@/app/e/[slug]/my-ticket";
import { Notice } from "@/components/form-parts";
import { SiteHeader } from "@/components/site-header";
import { formatEventDay, formatTimeRange } from "@/lib/format";
import { loadMyTicket } from "../../data";

export const metadata: Metadata = { title: "Biletin", robots: { index: false } };

type Props = { params: Promise<{ id: string }> };

/**
 * Etkinlik sayfası katılımcıya kapalıyken (sadece davetli, iptal edilmiş) bilet burada görünür.
 * Herkese açık etkinliklerde Hesabım'daki kart doğrudan etkinlik sayfasına gider.
 */
export default async function TicketPage({ params }: Props) {
    const ticket = await loadMyTicket((await params).id);
    if (!ticket) notFound();
    const { registration, place } = ticket;
    const event = registration.event ?? {};

    return (
        <>
            <SiteHeader />
            <main className="mx-auto max-w-xl px-5 pt-6 pb-20 sm:px-8">
                <Link
                    href="/hesabim"
                    className="text-sm text-muted-foreground underline-offset-4 hover:underline"
                >
                    Katıldığım etkinlikler
                </Link>
                <h1 className="mt-2 text-4xl leading-tight font-bold tracking-[-0.02em] [font-stretch:85%]">
                    {event.title}
                </h1>
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

                <section aria-label="Bilet" className="mt-8 rounded-xl border bg-card p-6">
                    {event.cancelled ? (
                        <p className="leading-relaxed">
                            Bu etkinlik organizatör tarafından iptal edildi; kaydın da iptal edildi.
                        </p>
                    ) : (
                        <>
                            {!event.publiclyVisible && (
                                <div className="mb-5">
                                    <Notice>
                                        Etkinlik sadece davetlilere açık; bilgileri ve biletin
                                        burada.
                                    </Notice>
                                </div>
                            )}
                            <MyTicket
                                registrationId={registration.id ?? ""}
                                ticketCode={registration.ticketCode}
                                status={registration.status}
                            />
                        </>
                    )}
                </section>
            </main>
        </>
    );
}
