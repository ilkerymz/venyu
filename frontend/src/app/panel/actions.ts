"use server";

import { refresh } from "next/cache";
import { redirect } from "next/navigation";
import { describeError, isApiError } from "@/api/api-error";
import { checkIn } from "@/api/generated/attendee-controller/attendee-controller";
import { invite } from "@/api/generated/invitation-controller/invitation-controller";
import {
    cancel1 as cancelEvent,
    create1 as createEvent,
    publish as publishEvent,
    update1 as updateEvent,
} from "@/api/generated/event-controller/event-controller";
import {
    _delete as deleteTicketType,
    add as addTicketType,
    update2 as updateTicketType,
} from "@/api/generated/ticket-type-controller/ticket-type-controller";
import { save as saveForm } from "@/api/generated/form-controller/form-controller";
import type {
    CreateEventRequestVisibility,
    FormField,
    TicketTypeRequest,
} from "@/api/generated/model";
import { labelErrors } from "@/lib/form-answers";
import type { FormState } from "@/auth/actions";
import { authHeaders } from "@/auth/server";
import { eventRange } from "@/lib/datetime";

/**
 * Organizatör işlemleri. Yetki (organizatör mü, etkinlik onun mu) backend'de kontrol edilir;
 * burada sadece form → istek dönüşümü ve hata mesajı var.
 */
const text = (form: FormData, name: string) => String(form.get(name) ?? "").trim();
const optional = (value: string) => value || undefined;
const visibility = (form: FormData) => text(form, "visibility") as CreateEventRequestVisibility;

function formValues(form: FormData): Record<string, string> {
    const values: Record<string, string> = {};
    form.forEach((value, key) => {
        if (typeof value === "string" && !key.startsWith("$")) values[key] = value;
    });
    return values;
}

/** Adresli etkinlik: tarih/saat formdan (mekanlı etkinlikte bu alanlar yok, rezervasyondan gelir). */
function addressAndTime(form: FormData) {
    const { startsAt, endsAt } = eventRange(
        text(form, "date"),
        text(form, "startTime"),
        text(form, "endTime"),
    );
    return { customAddress: text(form, "customAddress"), startsAt, endsAt };
}

/**
 * Etkinlik iki yerden birinde (ADR-007): onaylı bir mekan rezervasyonunda (zaman ve yer rezervasyondan,
 * toplam kapasite mekandan) ya da organizatörün yazdığı adreste.
 */
export async function createEventAction(_: FormState, form: FormData): Promise<FormState> {
    const venueBookingId = text(form, "venueBookingId");
    const common = {
        title: text(form, "title"),
        description: optional(text(form, "description")),
        visibility: visibility(form),
        requiresAccount: form.get("requiresAccount") === "on",
    };
    let id: string | undefined;
    try {
        const request = venueBookingId
            ? { ...common, venueBookingId }
            : { ...common, ...addressAndTime(form) };
        const { data } = await createEvent(request, { headers: await authHeaders() });
        id = data.id;
    } catch (error) {
        return { errors: describeError(error), values: formValues(form) };
    }
    redirect(`/panel/etkinlik/${id}?yeni=1`);
}

export async function updateEventAction(
    eventId: string,
    _: FormState,
    form: FormData,
): Promise<FormState> {
    try {
        await updateEvent(
            eventId,
            {
                title: text(form, "title"),
                description: optional(text(form, "description")),
                visibility: visibility(form),
                requiresAccount: form.get("requiresAccount") === "on",
            },
            { headers: await authHeaders() },
        );
    } catch (error) {
        return { errors: describeError(error), values: formValues(form) };
    }
    refresh();
    return { done: true };
}

export async function publishEventAction(eventId: string): Promise<FormState> {
    try {
        await publishEvent(eventId, { headers: await authHeaders() });
    } catch (error) {
        return { errors: describeError(error) };
    }
    refresh();
    return { done: true };
}

export async function cancelEventAction(
    eventId: string,
    _: FormState,
    form: FormData,
): Promise<FormState> {
    try {
        await cancelEvent(
            eventId,
            { reason: optional(text(form, "reason")) },
            { headers: await authHeaders() },
        );
    } catch (error) {
        return { errors: describeError(error) };
    }
    refresh();
    return { done: true };
}

function ticketRequest(form: FormData): TicketTypeRequest {
    const capacity = text(form, "capacity");
    return {
        name: text(form, "name"),
        description: optional(text(form, "description")),
        price: Number(text(form, "price").replace(",", ".") || 0),
        capacity: capacity ? Number(capacity) : undefined,
    };
}

export async function addTicketTypeAction(
    eventId: string,
    _: FormState,
    form: FormData,
): Promise<FormState> {
    try {
        await addTicketType(eventId, ticketRequest(form), { headers: await authHeaders() });
    } catch (error) {
        return { errors: describeError(error), values: formValues(form) };
    }
    refresh();
    return { done: true };
}

export async function updateTicketTypeAction(
    eventId: string,
    ticketTypeId: string,
    _: FormState,
    form: FormData,
): Promise<FormState> {
    try {
        await updateTicketType(eventId, ticketTypeId, ticketRequest(form), {
            headers: await authHeaders(),
        });
    } catch (error) {
        return { errors: describeError(error), values: formValues(form) };
    }
    refresh();
    return { done: true };
}

export async function deleteTicketTypeAction(
    eventId: string,
    ticketTypeId: string,
): Promise<FormState> {
    try {
        await deleteTicketType(eventId, ticketTypeId, { headers: await authHeaders() });
    } catch (error) {
        return { errors: describeError(error) };
    }
    refresh();
    return { done: true };
}

/** Formun tamamı kaydedilir; backend her kaydetmede yeni versiyon açar, boş liste formu kaldırır (ADR-009). */
export async function saveFormAction(
    eventId: string,
    _: FormState,
    fields: FormField[],
): Promise<FormState> {
    try {
        await saveForm(eventId, { fields }, { headers: await authHeaders() });
    } catch (error) {
        return { errors: labelErrors(describeError(error), fields) };
    }
    refresh();
    return { done: true };
}

export type CheckInResult =
    | { kind: "ok"; name: string; at?: string }
    | { kind: "already"; at?: string }
    | { kind: "invalid"; message: string };

/**
 * Kapıda bilet okutma (ADR-010): tek koşullu UPDATE, aynı bilet ikinci kez 409 + `checkedInAt`.
 * Başka etkinliğin ya da olmayan kod 404, iptal edilmiş kayıt veya kapı penceresi dışı 409.
 */
export async function checkInAction(eventId: string, ticketCode: string): Promise<CheckInResult> {
    try {
        const { data } = await checkIn(
            eventId,
            { ticketCode: ticketCode.trim() },
            { headers: await authHeaders() },
        );
        return { kind: "ok", name: data.name ?? "", at: data.checkedInAt };
    } catch (error) {
        if (isApiError(error, 409) && error.problem.checkedInAt) {
            return { kind: "already", at: error.problem.checkedInAt };
        }
        if (isApiError(error, 404))
            return { kind: "invalid", message: "Bu bilet bu etkinliğe ait değil." };
        return { kind: "invalid", message: describeError(error)[0] };
    }
}

/** Toplu davet (ADR-008): aynı adrese tekrar göndermek güvenli, backend idempotent. */
export async function inviteAction(eventId: string, emails: string[]): Promise<FormState> {
    try {
        await invite(eventId, { emails }, { headers: await authHeaders() });
    } catch (error) {
        return { errors: describeError(error) };
    }
    refresh();
    return { done: true };
}
