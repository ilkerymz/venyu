"use server";

import { refresh } from "next/cache";
import { describeError } from "@/api/api-error";
import { hold } from "@/api/generated/venue-booking-controller/venue-booking-controller";
import type { FormState } from "@/auth/actions";
import { authHeaders } from "@/auth/server";

/** Organizatör boş saati 24 saatliğine tutar (HELD); mekan onaylayınca CONFIRMED olur (ADR-006). */
export async function holdSlotAction(
    venueId: string,
    slotId: string,
    _: FormState,
    form: FormData,
): Promise<FormState> {
    try {
        const note = String(form.get("note") ?? "").trim() || undefined;
        await hold(venueId, slotId, { note }, { headers: await authHeaders() });
    } catch (error) {
        return { errors: describeError(error), values: { note: String(form.get("note") ?? "") } };
    }
    refresh();
    return { done: true };
}
