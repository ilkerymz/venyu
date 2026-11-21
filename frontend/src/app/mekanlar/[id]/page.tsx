import { MapPin, Users } from "lucide-react";
import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { cache } from "react";
import { isApiError } from "@/api/api-error";
import { me } from "@/api/generated/me-controller/me-controller";
import { get as getVenue } from "@/api/generated/venue-controller/venue-controller";
import { loadSlots } from "@/app/mekan/data";
import { authHeaders, hasSession } from "@/auth/server";
import { SiteHeader } from "@/components/site-header";
import { SlotPicker, type Visitor } from "./slot-picker";

type Props = { params: Promise<{ id: string }> };

const loadVenue = cache(async (id: string) => {
    try {
        return (await getVenue(id)).data;
    } catch (error) {
        if (isApiError(error, 404)) notFound();
        throw error;
    }
});

export async function generateMetadata({ params }: Props): Promise<Metadata> {
    const venue = await loadVenue((await params).id).catch(() => undefined);
    return venue ? { title: venue.name, description: venue.description?.slice(0, 160) } : {};
}

/** Ziyaretçinin rolü: saati sadece organizatör ayırabilir (`hold` ORGANIZER ister). */
async function visitor(): Promise<Visitor> {
    if (!(await hasSession())) return "guest";
    const profile = await me({ headers: await authHeaders() })
        .then((r) => r.data)
        .catch(() => undefined);
    return profile?.roles?.includes("ORGANIZER") ? "organizer" : "member";
}

export default async function VenuePage({ params }: Props) {
    const { id } = await params;
    const [venue, slots, who] = await Promise.all([
        loadVenue(id),
        loadSlots(id).then((all) => all.filter((s) => s.available)),
        visitor(),
    ]);

    return (
        <>
            <SiteHeader />
            <main className="mx-auto grid max-w-5xl gap-10 px-5 pt-6 pb-20 sm:px-8 lg:grid-cols-[1fr_24rem]">
                <div>
                    <h1 className="text-4xl font-bold tracking-[-0.02em] [font-stretch:85%] sm:text-5xl">
                        {venue.name}
                    </h1>
                    <p className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-muted-foreground">
                        <span className="flex items-center gap-1.5">
                            <MapPin className="size-4" aria-hidden />
                            {[venue.district, venue.city].filter(Boolean).join(", ")}
                        </span>
                        {venue.capacity && (
                            <span className="flex items-center gap-1.5 nums">
                                <Users className="size-4" aria-hidden />
                                {venue.capacity} kişi
                            </span>
                        )}
                    </p>
                    {venue.description && (
                        <p className="mt-6 max-w-prose leading-relaxed whitespace-pre-line">
                            {venue.description}
                        </p>
                    )}
                    <section className="mt-10">
                        <h2 className="text-xl font-semibold">Adres</h2>
                        <address className="mt-2 leading-relaxed not-italic">
                            {venue.address}
                            <br />
                            <span className="text-muted-foreground">
                                {[venue.district, venue.city].filter(Boolean).join(", ")}
                            </span>
                        </address>
                    </section>
                </div>
                <aside aria-labelledby="slots-heading" className="lg:self-start">
                    <div className="rounded-xl border bg-card p-5">
                        <h2 id="slots-heading" className="text-xl font-semibold">
                            Boş saatler
                        </h2>
                        <SlotPicker
                            venueId={id}
                            venueName={venue.name ?? ""}
                            slots={slots}
                            visitor={who}
                        />
                    </div>
                </aside>
            </main>
        </>
    );
}
