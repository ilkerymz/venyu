import "server-only";
import { notFound, redirect } from "next/navigation";
import { cache } from "react";
import { isApiError } from "@/api/api-error";
import {
    get1 as getEvent,
    listMine1 as listMyEvents,
} from "@/api/generated/event-controller/event-controller";
import { list2 as listTicketTypes } from "@/api/generated/ticket-type-controller/ticket-type-controller";
import { get2 as getForm } from "@/api/generated/form-controller/form-controller";
import { list3 as listAttendees } from "@/api/generated/attendee-controller/attendee-controller";
import { list4 as listInvitations } from "@/api/generated/invitation-controller/invitation-controller";
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
