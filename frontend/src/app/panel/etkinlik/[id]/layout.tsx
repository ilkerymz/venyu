import { ExternalLink, MapPin } from "lucide-react";
import Link from "next/link";
import { Suspense } from "react";
import { EventStatusBadge } from "@/components/event-status-badge";
import { SiteHeader } from "@/components/site-header";
import { formatEventDay, formatTimeRange } from "@/lib/format";
import { get as getVenue } from "@/api/generated/venue-controller/venue-controller";
import { loadEventSummary } from "../../data";
import { EventTabs } from "./event-tabs";

type Props = { children: React.ReactNode; params: Promise<{ id: string }> };

/**
 * Etkinlik yönetiminin ortak çerçevesi: başlık + sekmeler. Etkinlik bilgisi Suspense içinde
 * yüklenir; sekmeler ve iskelet anında görünür (instant navigation).
 */
export default function ManageEventLayout({ children, params }: Props) {
    return (
        <>
            <SiteHeader />
            <main className="mx-auto max-w-6xl px-5 pt-6 pb-20 sm:px-8">
                <Link
                    href="/hesabim"
                    className="text-sm text-muted-foreground underline-offset-4 hover:underline"
                >
                    Etkinliklerim
                </Link>
                <Suspense fallback={<HeaderSkeleton />}>
                    <EventHeader params={params} />
                </Suspense>
                <EventTabs />
                <div className="mt-8">{children}</div>
            </main>
        </>
    );
}

async function EventHeader({ params }: { params: Promise<{ id: string }> }) {
    const event = await loadEventSummary((await params).id);
    // Mekanlı etkinlikte yer mekandan (ADR-007), adresli etkinlikte adresin ilk satırı.
    const venue = event.venueId
        ? await getVenue(event.venueId)
              .then((r) => r.data)
              .catch(() => undefined)
        : undefined;
    const place = venue
        ? [venue.name, venue.district].filter(Boolean).join(", ")
        : event.customAddress?.split("\n")[0];
    return (
        <div className="mt-2">
            <div className="flex flex-wrap items-center gap-3">
                <h1 className="text-4xl font-bold tracking-[-0.02em] [font-stretch:85%] sm:text-5xl">
                    {event.title}
                </h1>
                <EventStatusBadge status={event.status} />
            </div>
            <p className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-muted-foreground nums">
                <span>
                    {formatEventDay(event.startsAt ?? "")},{" "}
                    {formatTimeRange(event.startsAt ?? "", event.endsAt ?? "")}
                </span>
                {place && (
                    <span className="flex items-center gap-1.5">
                        <MapPin className="size-4 shrink-0" aria-hidden />
                        {place}
                    </span>
                )}
                <Link
                    href={`/e/${event.slug}`}
                    className="flex items-center gap-1.5 text-primary underline-offset-4 hover:underline"
                >
                    <ExternalLink className="size-4 shrink-0" aria-hidden />
                    Etkinlik sayfası
                </Link>
            </p>
        </div>
    );
}

function HeaderSkeleton() {
    return (
        <div className="mt-2" aria-busy aria-label="Etkinlik yükleniyor">
            <div className="h-12 w-80 max-w-full rounded-md bg-muted motion-safe:animate-pulse" />
            <div className="mt-3 h-5 w-64 rounded-md bg-muted motion-safe:animate-pulse" />
        </div>
    );
}
