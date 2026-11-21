"use server";

import { refresh } from "next/cache";
import { redirect } from "next/navigation";
import { describeError } from "@/api/api-error";
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

export async function createEventAction(_: FormState, form: FormData): Promise<FormState> {
    const { startsAt, endsAt } = eventRange(
        text(form, "date"),
        text(form, "startTime"),
        text(form, "endTime"),
    );
    let id: string | undefined;
    try {
        const { data } = await createEvent(
            {
                title: text(form, "title"),
                description: optional(text(form, "description")),
                visibility: visibility(form),
                requiresAccount: form.get("requiresAccount") === "on",
                customAddress: text(form, "customAddress"),
                startsAt,
                endsAt,
            },
            { headers: await authHeaders() },
        );
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
