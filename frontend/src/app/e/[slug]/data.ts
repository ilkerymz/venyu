import { notFound } from "next/navigation";
import { cache } from "react";
import { isApiError } from "@/api/api-error";
import { getBySlug } from "@/api/generated/event-controller/event-controller";
import { get as getVenue } from "@/api/generated/venue-controller/venue-controller";

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
