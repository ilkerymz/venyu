import { notFound } from "next/navigation";
import { cache } from "react";
import { isApiError } from "@/api/api-error";
import { getBySlug } from "@/api/generated/event-controller/event-controller";
import { get as getVenue } from "@/api/generated/venue-controller/venue-controller";
import { get2 as getForm } from "@/api/generated/form-controller/form-controller";
import { listMine as listMyRegistrations } from "@/api/generated/registration-controller/registration-controller";
import { authHeaders, hasSession } from "@/auth/server";

/**
 * Sayfa ve metadata aynı veriyi kullanır; `cache` istek başına tek çağrı yapar.
 * Backend görünmeyen etkinlikte (yok, taslak, sadece davetli) 404 döner → Next.js 404 sayfası.
 */
export const loadPublicEvent = cache(async (slug: string) => {
    try {
        const { data: event } = await getBySlug(slug);
        const venue = event.venueId ? (await getVenue(event.venueId)).data : null;
        return { event, venue };
    } catch (error) {
        if (isApiError(error, 404)) notFound();
        throw error;
    }
});

const ACTIVE = new Set(["RESERVED", "CONFIRMED", "CHECKED_IN"]);

/** Kayıt paneli için: formun soruları ve (oturum varsa) kullanıcının bu etkinlikteki aktif kaydı. */
export async function loadRegistrationContext(eventId: string) {
    const [fields, session] = await Promise.all([
        getForm(eventId)
            .then((r) => r.data.fields ?? [])
            .catch(() => []),
        hasSession(),
    ]);
    if (!session) return { fields, isMember: false, myRegistration: undefined };
    const mine = await listMyRegistrations({ headers: await authHeaders() })
        .then((r) => r.data ?? [])
        .catch(() => []);
    const myRegistration = mine.find((r) => r.eventId === eventId && ACTIVE.has(r.status ?? ""));
    return { fields, isMember: true, myRegistration };
}
