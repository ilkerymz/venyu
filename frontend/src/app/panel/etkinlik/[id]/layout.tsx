import { Suspense } from "react";
import { EventHero } from "@/components/event-hero";
import { OrganizerBar } from "@/components/organizer-bar";
import { SiteHeader } from "@/components/site-header";
import { loadEventSummary, loadVenue } from "../../data";

type Props = { children: React.ReactNode; params: Promise<{ id: string }> };

/**
 * Etkinlik yönetiminin çerçevesi, etkinlik sayfasıyla aynı yapı: üstte organizatör şeridi (sekmeler),
 * altında bilet koçanı başlığı, en altta sekmenin içeriği. Şerit ve başlık Suspense içinde yüklenir;
 * sekmeler arasında gezerken layout yeniden çizilmez, sadece içerik değişir.
 */
export default function ManageEventLayout({ children, params }: Props) {
    return (
        <>
            <SiteHeader />
            <Suspense fallback={<ChromeSkeleton />}>
                <EventChrome params={params} />
            </Suspense>
            <main className="mx-auto w-full max-w-6xl px-5 pt-10 pb-20 sm:px-8">{children}</main>
        </>
    );
}

async function EventChrome({ params }: { params: Promise<{ id: string }> }) {
    const event = await loadEventSummary((await params).id);
    // Mekanlı etkinlikte yer mekandan (ADR-007), adresli etkinlikte adresin ilk satırı.
    const venue = event.venueId ? await loadVenue(event.venueId) : undefined;
    const place = venue
        ? [venue.name, venue.district].filter(Boolean).join(", ")
        : event.customAddress?.split("\n")[0];
    return (
        <>
            <OrganizerBar event={event} className="pt-6" />
            <div className="mx-auto max-w-6xl px-5 pt-4 sm:px-8 lg:pt-8">
                <EventHero
                    title={event.title ?? ""}
                    startsAt={event.startsAt ?? ""}
                    endsAt={event.endsAt ?? ""}
                    place={place}
                    className="lg:max-w-[calc(100%-25rem)]"
                />
            </div>
        </>
    );
}

function ChromeSkeleton() {
    return (
        <div
            className="mx-auto max-w-6xl px-5 pt-6 sm:px-8"
            aria-busy
            aria-label="Etkinlik yükleniyor"
        >
            <div className="h-14 rounded-xl bg-muted motion-safe:animate-pulse" />
            <div className="mt-4 h-40 rounded-xl bg-muted motion-safe:animate-pulse lg:mt-8 lg:max-w-[calc(100%-25rem)]" />
        </div>
    );
}
