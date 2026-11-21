"use server";

import { refresh } from "next/cache";
import { redirect } from "next/navigation";
import { describeError } from "@/api/api-error";
import {
    confirm1 as confirmBooking,
    reject as rejectBooking,
} from "@/api/generated/venue-booking-controller/venue-booking-controller";
import {
    create as createVenue,
    update as updateVenue,
} from "@/api/generated/venue-controller/venue-controller";
import {
    close as closeSlot,
    open as openSlot,
} from "@/api/generated/venue-slot-controller/venue-slot-controller";
import type { VenueRequest } from "@/api/generated/model";
import type { FormState } from "@/auth/actions";
import { authHeaders } from "@/auth/server";
import { eventRange } from "@/lib/datetime";

/** Mekan sahibi işlemleri (ADR-006). Sahiplik ve durum kuralları backend'de; burada form → istek. */
const text = (form: FormData, name: string) => String(form.get(name) ?? "").trim();

function venueRequest(form: FormData): VenueRequest {
    const capacity = text(form, "capacity");
    return {
        name: text(form, "name"),
        description: text(form, "description") || undefined,
        address: text(form, "address"),
        district: text(form, "district"),
        city: text(form, "city"),
        capacity: capacity ? Number(capacity) : undefined,
    };
}

function values(form: FormData) {
    return Object.fromEntries(
        [...form.entries()].filter(([k, v]) => typeof v === "string" && !k.startsWith("$")),
    ) as Record<string, string>;
}

export async function createVenueAction(_: FormState, form: FormData): Promise<FormState> {
    let id: string | undefined;
    try {
        id = (await createVenue(venueRequest(form), { headers: await authHeaders() })).data.id;
    } catch (error) {
        return { errors: describeError(error), values: values(form) };
    }
    redirect(`/mekan/${id}`);
}

export async function updateVenueAction(
    venueId: string,
    _: FormState,
    form: FormData,
): Promise<FormState> {
    try {
        await updateVenue(venueId, venueRequest(form), { headers: await authHeaders() });
    } catch (error) {
        return { errors: describeError(error), values: values(form) };
    }
    refresh();
    return { done: true };
}

export async function openSlotAction(
    venueId: string,
    _: FormState,
    form: FormData,
): Promise<FormState> {
    const { startsAt, endsAt } = eventRange(
        text(form, "date"),
        text(form, "startTime"),
        text(form, "endTime"),
    );
    try {
        await openSlot(venueId, { startsAt, endsAt }, { headers: await authHeaders() });
    } catch (error) {
        return { errors: describeError(error), values: values(form) };
    }
    refresh();
    return { done: true };
}

export async function closeSlotAction(venueId: string, slotId: string): Promise<FormState> {
    try {
        await closeSlot(venueId, slotId, { headers: await authHeaders() });
    } catch (error) {
        return { errors: describeError(error) };
    }
    refresh();
    return { done: true };
}

export async function decideBookingAction(
    bookingId: string,
    decision: "confirm" | "reject",
): Promise<FormState> {
    try {
        const headers = await authHeaders();
        await (decision === "confirm"
            ? confirmBooking(bookingId, { headers })
            : rejectBooking(bookingId, { headers }));
    } catch (error) {
        return { errors: describeError(error) };
    }
    refresh();
    return { done: true };
}
