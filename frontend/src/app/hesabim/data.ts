import "server-only";
import { guard } from "@/api/guard";
import type { EventResponse, MyRegistrationResponse } from "@/api/generated/model";
import { listMine as listMyRegistrations } from "@/api/generated/registration-controller/registration-controller";
import { loadMyBookings, loadMyEvents, loadVenue } from "@/app/panel/data";
import { authHeaders } from "@/auth/server";
import { splitByTime } from "@/lib/datetime";
import { type TicketState, visibleRegistrations } from "@/lib/registrations";

export type EventCardView = { event: EventResponse; place?: string };

/**
 * Hesabım'daki "Etkinliklerim": organizatörün etkinlikleri (yer adıyla) yaklaşan / geçmiş diye
 * ayrılmış, artı mekan rezervasyonları. Çağrıdan önce sayfa cookie okuduğu için `Date.now()` güvenli.
 */
export async function loadOrganizerArea() {
    const [events, bookings] = await Promise.all([loadMyEvents(), loadMyBookings()]);
    // loadVenue istek başına önbellekli: aynı mekan bir kez okunur.
    const cards: EventCardView[] = await Promise.all(
        events.map(async (event) => ({ event, place: await placeOf(event) })),
    );
    return {
        ...splitByTime(cards, (c) => c.event, Date.now()),
        bookings,
    };
}

/** Mekan adı + semt ya da adresin ilk satırı (kartlarda kısa yer). */
async function placeOf(event: { venueId?: string; customAddress?: string }) {
    if (!event.venueId) return event.customAddress?.split("\n")[0];
    const venue = await loadVenue(event.venueId);
    return venue ? [venue.name, venue.district].filter(Boolean).join(", ") : undefined;
}

export type TicketCardView = {
    registration: MyRegistrationResponse;
    state: TicketState;
    place?: string;
};

/** "Katıldığım etkinlikler": görünür kayıtlar (etkinlik başına bir), yer adıyla, yaklaşan / geçmiş. */
export async function loadMyTickets() {
    const headers = await authHeaders();
    const registrations = await guard(
        async () => (await listMyRegistrations({ headers })).data ?? [],
        "/hesabim",
    );
    const cards: TicketCardView[] = await Promise.all(
        visibleRegistrations(registrations).map(async ({ registration, state }) => ({
            registration,
            state,
            place: await placeOf(registration.event ?? {}),
        })),
    );
    return splitByTime(cards, (c) => c.registration.event ?? {}, Date.now());
}

/** Bilet sayfası: kullanıcının kayıtlarından biri (başkasınınki ya da yoksa `undefined`). */
export async function loadMyTicket(registrationId: string) {
    const headers = await authHeaders();
    const registrations = await guard(
        async () => (await listMyRegistrations({ headers })).data ?? [],
        `/hesabim/bilet/${registrationId}`,
    );
    const registration = registrations.find((r) => r.id === registrationId);
    if (!registration?.event) return undefined;
    return { registration, place: await placeOf(registration.event) };
}
