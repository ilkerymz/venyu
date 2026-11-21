import "server-only";
import { notFound } from "next/navigation";
import { connection } from "next/server";
import { cache } from "react";
import { guard } from "@/api/guard";
import { me } from "@/api/generated/me-controller/me-controller";
import { listForVenue } from "@/api/generated/venue-booking-controller/venue-booking-controller";
import {
    get as getVenue,
    list as listVenues,
} from "@/api/generated/venue-controller/venue-controller";
import { list1 as listSlots } from "@/api/generated/venue-slot-controller/venue-slot-controller";
import { authHeaders } from "@/auth/server";

const DAY = 24 * 60 * 60 * 1000;
/** Takvimde gösterilen pencere: bugünden itibaren 60 gün (backend varsayılanından uzun). */
export const SLOT_WINDOW_DAYS = 60;

export const loadMe = cache(async () => {
    const headers = await authHeaders();
    return guard(async () => (await me({ headers })).data, "/mekan");
});

export async function loadMyVenues() {
    const profile = await loadMe();
    const isOwner = profile.roles?.includes("VENUE_OWNER") ?? false;
    if (!isOwner) return { isOwner, venues: [] };
    const venues = (await listVenues()).data ?? [];
    return { isOwner, venues: venues.filter((v) => v.ownerId === profile.id) };
}

/** Mekan herkese açık okunur; panelde sadece sahibine gösterilir (başkasınınki 404). */
export const loadOwnedVenue = cache(async (venueId: string) => {
    const [profile, venue] = await Promise.all([
        loadMe(),
        guard(async () => (await getVenue(venueId)).data, `/mekan/${venueId}`),
    ]);
    if (venue.ownerId !== profile.id) notFound();
    return venue;
});

export async function loadSlots(venueId: string, fromDaysAgo = 0) {
    await connection(); // "şimdi" istek anına ait; prerender sırasında hesaplanmamalı.
    const from = new Date(Date.now() - fromDaysAgo * DAY).toISOString();
    const to = new Date(Date.now() + SLOT_WINDOW_DAYS * DAY).toISOString();
    return (await listSlots(venueId, { from, to })).data ?? [];
}

export async function loadBookings(venueId: string) {
    const headers = await authHeaders();
    return guard(
        async () => (await listForVenue(venueId, { headers })).data ?? [],
        `/mekan/${venueId}/rezervasyonlar`,
    );
}
