import { MapPin, Users } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { list as listVenues } from "@/api/generated/venue-controller/venue-controller";
import { SiteHeader } from "@/components/site-header";
import { cn } from "@/lib/utils";

export const metadata: Metadata = {
    title: "Mekanlar",
    description: "Etkinliğin için boş saati olan mekanları bul: kafe, atölye, bar.",
};

type Props = { searchParams: Promise<{ semt?: string }> };

export default function VenuesPage({ searchParams }: Props) {
    return (
        <>
            <SiteHeader />
            <main className="mx-auto max-w-5xl px-5 pt-6 pb-20 sm:px-8">
                <h1 className="text-4xl font-bold tracking-[-0.02em] [font-stretch:85%] sm:text-5xl">
                    Mekanlar
                </h1>
                <p className="mt-3 max-w-xl leading-relaxed text-muted-foreground">
                    Etkinliğin için boş saati olan mekanlar. Bir mekanın takviminden saat seç, mekan
                    onaylayınca etkinliğini oraya kur.
                </p>
                <Suspense
                    fallback={
                        <div className="mt-10 h-64 rounded-xl bg-muted motion-safe:animate-pulse" />
                    }
                >
                    <VenueDirectory searchParams={searchParams} />
                </Suspense>
            </main>
        </>
    );
}

async function VenueDirectory({ searchParams }: Props) {
    const [{ semt }, venues] = await Promise.all([
        searchParams,
        listVenues().then((r) => r.data ?? []),
    ]);
    const districts = [...new Set(venues.map((v) => v.district ?? "").filter(Boolean))].sort(
        (a, b) => a.localeCompare(b, "tr"),
    );
    const shown = semt ? venues.filter((v) => v.district === semt) : venues;

    return (
        <>
            {districts.length > 1 && (
                <nav aria-label="Semt" className="mt-8 flex flex-wrap gap-2">
                    {[undefined, ...districts].map((d) => (
                        <Link
                            key={d ?? "hepsi"}
                            href={d ? `/mekanlar?semt=${encodeURIComponent(d)}` : "/mekanlar"}
                            aria-current={semt === d ? "page" : undefined}
                            className={cn(
                                "rounded-full border px-3 py-1 text-sm outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50",
                                semt === d
                                    ? "border-primary bg-primary text-primary-foreground"
                                    : "bg-card hover:bg-muted",
                            )}
                        >
                            {d ?? "Hepsi"}
                        </Link>
                    ))}
                </nav>
            )}
            {shown.length === 0 ? (
                <p className="mt-10 text-muted-foreground">Henüz listelenen mekan yok.</p>
            ) : (
                <ul className="mt-8 grid gap-4 sm:grid-cols-2">
                    {shown.map((venue) => (
                        <li key={venue.id}>
                            <Link
                                href={`/mekanlar/${venue.id}`}
                                className="flex h-full flex-col rounded-xl border bg-card p-5 outline-none hover:border-primary focus-visible:ring-[3px] focus-visible:ring-ring/50"
                            >
                                <span className="font-display text-xl font-semibold">
                                    {venue.name}
                                </span>
                                <span className="mt-1 flex flex-wrap gap-x-4 text-sm text-muted-foreground">
                                    <span className="flex items-center gap-1">
                                        <MapPin className="size-3.5" aria-hidden />
                                        {venue.district}
                                    </span>
                                    {venue.capacity && (
                                        <span className="flex items-center gap-1 nums">
                                            <Users className="size-3.5" aria-hidden />
                                            {venue.capacity} kişi
                                        </span>
                                    )}
                                </span>
                                {venue.description && (
                                    <span className="mt-3 line-clamp-2 text-sm leading-relaxed text-muted-foreground">
                                        {venue.description}
                                    </span>
                                )}
                            </Link>
                        </li>
                    ))}
                </ul>
            )}
        </>
    );
}
