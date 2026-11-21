import "server-only";
import type { EventResponse } from "@/api/generated/model";
import { loadMyBookings, loadMyEvents, loadVenue } from "@/app/panel/data";
import { splitByTime } from "@/lib/datetime";

export type EventCardView = { event: EventResponse; place?: string };

/**
 * Hesabım'daki "Etkinliklerim": organizatörün etkinlikleri (yer adıyla) yaklaşan / geçmiş diye
 * ayrılmış, artı mekan rezervasyonları. Çağrıdan önce sayfa cookie okuduğu için `Date.now()` güvenli.
 */
export async function loadOrganizerArea() {
    const [events, bookings] = await Promise.all([loadMyEvents(), loadMyBookings()]);
    const venues = new Map(
        await Promise.all(
            [...new Set(events.map((e) => e.venueId).filter((id) => id != null))].map(
                async (id) => [id, await loadVenue(id)] as const,
            ),
        ),
    );
    const cards: EventCardView[] = events.map((event) => {
        const venue = event.venueId ? venues.get(event.venueId) : undefined;
        const place = venue
            ? [venue.name, venue.district].filter(Boolean).join(", ")
            : event.customAddress?.split("\n")[0];
        return { event, place };
    });
    return {
        ...splitByTime(cards, (c) => c.event, Date.now()),
        bookings,
    };
}
