"use server";

import { refresh } from "next/cache";
import { describeError } from "@/api/api-error";
import {
    cancelByManageToken,
    confirm,
} from "@/api/generated/registration-controller/registration-controller";

export type ConfirmState = { errors?: string[]; ticketCode?: string };

/** Misafirin e-posta onayı (ADR-008): RESERVED → CONFIRMED, bilet kodu üretilir ve e-postayla gelir. */
export async function confirmRegistrationAction(
    _: ConfirmState,
    form: FormData,
): Promise<ConfirmState> {
    try {
        const { data } = await confirm({ token: String(form.get("token") ?? "") });
        return { ticketCode: data.ticketCode };
    } catch (error) {
        return { errors: describeError(error) };
    }
}

export async function cancelManagedAction(token: string): Promise<{ errors?: string[] }> {
    try {
        await cancelByManageToken({ token });
    } catch (error) {
        return { errors: describeError(error) };
    }
    refresh();
    return {};
}
