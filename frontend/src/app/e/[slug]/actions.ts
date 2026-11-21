"use server";

import { refresh } from "next/cache";
import { describeError } from "@/api/api-error";
import { get2 as getForm } from "@/api/generated/form-controller/form-controller";
import type { FormField } from "@/api/generated/model";
import {
    cancel,
    register,
    registerGuest,
} from "@/api/generated/registration-controller/registration-controller";
import { authHeaders } from "@/auth/server";
import { labelErrors, readAnswers } from "@/lib/form-answers";

import type { RegisterState } from "@/components/registration-panel";

async function loadFields(eventId: string): Promise<FormField[]> {
    try {
        return (await getForm(eventId)).data.fields ?? [];
    } catch {
        return []; // form yoksa (veya okunamazsa) cevapsız kayıt; backend yine doğrular.
    }
}

/**
 * Kayıt: oturum varsa üye kaydı (ücretsiz bilette hemen CONFIRMED + bilet kodu), yoksa misafir
 * kaydı (RESERVED; 30 dk içinde e-postadaki bağlantıyla onaylanmalı, ADR-008).
 */
export async function registerAction(
    eventId: string,
    _: RegisterState,
    form: FormData,
): Promise<RegisterState> {
    const fields = await loadFields(eventId);
    const ticketTypeId = String(form.get("ticketTypeId") ?? "");
    const answers = readAnswers(fields, form);
    const headers = await authHeaders();
    const isMember = "authorization" in headers;

    try {
        if (isMember) {
            await register(eventId, { ticketTypeId, answers }, { headers });
        } else {
            const email = String(form.get("guestEmail") ?? "").trim();
            await registerGuest(eventId, {
                ticketTypeId,
                answers,
                email,
                name: String(form.get("guestName") ?? "").trim(),
            });
            return { guestEmail: email };
        }
    } catch (error) {
        return { errors: labelErrors(describeError(error), fields) };
    }
    refresh();
    return {};
}

export async function cancelMyRegistrationAction(
    registrationId: string,
): Promise<{ errors?: string[] }> {
    try {
        await cancel(registrationId, { headers: await authHeaders() });
    } catch (error) {
        return { errors: describeError(error) };
    }
    refresh();
    return {};
}
