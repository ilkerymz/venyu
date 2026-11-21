import "server-only";
import { cache } from "react";
import { guard } from "@/api/guard";
import {
    get1 as getEvent,
    listMine1 as listMyEvents,
} from "@/api/generated/event-controller/event-controller";
import { list2 as listTicketTypes } from "@/api/generated/ticket-type-controller/ticket-type-controller";
import { get2 as getForm } from "@/api/generated/form-controller/form-controller";
import { list3 as listAttendees } from "@/api/generated/attendee-controller/attendee-controller";
import { list4 as listInvitations } from "@/api/generated/invitation-controller/invitation-controller";
import { listMine2 as listMyBookings } from "@/api/generated/venue-booking-controller/venue-booking-controller";
import { get as getVenue } from "@/api/generated/venue-controller/venue-controller";
import { list1 as listSlots } from "@/api/generated/venue-slot-controller/venue-slot-controller";
import type {
    BookingResponse,
    EventResponse,
    SlotResponse,
    VenueResponse,
} from "@/api/generated/model";
import { authHeaders } from "@/auth/server";

export async function loadMyEvents() {
    const headers = await authHeaders();
    return guard(async () => (await listMyEvents({ headers })).data ?? [], "/panel");
}

export async function loadManagedEvent(eventId: string) {
    const headers = await authHeaders();
    return guard(async () => {
        const [event, tickets, form] = await Promise.all([
            loadEventSummary(eventId).then((data) => ({ data })),
            listTicketTypes(eventId, { headers }),
            getForm(eventId, { headers }),
        ]);
        return {
            event: event.data,
            tickets: tickets.data ?? [],
            questionCount: form.data.fields?.length ?? 0,
        };
    }, `/panel/etkinlik/${eventId}`);
}

/** Sekmelerin ortak başlığı ve alt sayfalar aynı istekte etkinliği bir kez yükler. */
export const loadEventSummary = cache(async (eventId: string) => {
    const headers = await authHeaders();
    return guard(
        async () => (await getEvent(eventId, { headers })).data,
        `/panel/etkinlik/${eventId}`,
    );
});

export async function loadAttendees(eventId: string) {
    const headers = await authHeaders();
    return guard(async () => {
        const [attendees, form] = await Promise.all([
            listAttendees(eventId, { headers }),
            getForm(eventId, { headers }),
        ]);
        return { attendees: attendees.data ?? [], fields: form.data.fields ?? [] };
    }, `/panel/etkinlik/${eventId}/katilimcilar`);
}

export async function loadInvitations(eventId: string) {
    const headers = await authHeaders();
    return guard(
        async () => (await listInvitations(eventId, { headers })).data ?? [],
        `/panel/etkinlik/${eventId}/davetler`,
    );
}

export type BookingView = {
    booking: BookingResponse;
    venue?: VenueResponse;
    slot?: SlotResponse;
    /** Bu rezervasyonla oluşturulmuş etkinlik (rezervasyon başına en fazla bir, UNIQUE). */
    event?: EventResponse;
};

const DAY = 24 * 60 * 60 * 1000;

/**
 * Organizatörün mekan rezervasyonları; mekan adı, saat ve bağlı etkinlikle birlikte.
 * `BookingResponse` sadece id taşıdığı için mekan ve slot ayrıca okunur (mekan başına bir istek).
 */
export async function loadMyBookings(): Promise<BookingView[]> {
    const headers = await authHeaders();
    const [bookings, events] = await Promise.all([
        guard(async () => (await listMyBookings({ headers })).data ?? [], "/panel"),
        loadMyEvents(),
    ]);
    const venueIds = [...new Set(bookings.map((b) => b.venueId ?? ""))];
    const from = new Date(Date.now() - 30 * DAY).toISOString();
    const to = new Date(Date.now() + 120 * DAY).toISOString();
    const perVenue = await Promise.all(
        venueIds.map(async (id) => {
            const [venue, slots] = await Promise.all([
                getVenue(id)
                    .then((r) => r.data)
                    .catch(() => undefined),
                listSlots(id, { from, to })
                    .then((r) => r.data ?? [])
                    .catch(() => []),
            ]);
            return { id, venue, slots };
        }),
    );
    const venues = new Map(perVenue.map((v) => [v.id, v]));
    return bookings.map((booking) => {
        const v = venues.get(booking.venueId ?? "");
        return {
            booking,
            venue: v?.venue,
            slot: v?.slots.find((s) => s.id === booking.slotId),
            event: events.find((e) => e.venueBookingId === booking.id),
        };
    });
}
