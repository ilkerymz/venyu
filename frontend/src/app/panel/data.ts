import "server-only";
import { notFound, redirect } from "next/navigation";
import { isApiError } from "@/api/api-error";
import {
    get1 as getEvent,
    listMine1 as listMyEvents,
} from "@/api/generated/event-controller/event-controller";
import { list2 as listTicketTypes } from "@/api/generated/ticket-type-controller/ticket-type-controller";
import { authHeaders } from "@/auth/server";

/** 401 → giriş, 403 → organizatör değil (hesap sayfası rolü almayı sunar), 404 → yok ya da başkasının. */
async function guard<T>(load: () => Promise<T>, next: string): Promise<T> {
    try {
        return await load();
    } catch (error) {
        if (isApiError(error, 401)) redirect(`/giris?next=${encodeURIComponent(next)}`);
        if (isApiError(error, 403)) redirect("/hesabim");
        if (isApiError(error, 404)) notFound();
        throw error;
    }
}

export async function loadMyEvents() {
    const headers = await authHeaders();
    return guard(async () => (await listMyEvents({ headers })).data ?? [], "/panel");
}

export async function loadManagedEvent(eventId: string) {
    const headers = await authHeaders();
    return guard(async () => {
        const [event, tickets] = await Promise.all([
            getEvent(eventId, { headers }),
            listTicketTypes(eventId, { headers }),
        ]);
        return { event: event.data, tickets: tickets.data ?? [] };
    }, `/panel/etkinlik/${eventId}`);
}
