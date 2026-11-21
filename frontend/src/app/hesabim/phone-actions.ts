"use server";

import { refresh } from "next/cache";
import { describeError } from "@/api/api-error";
import {
    confirmCode,
    requestCode,
} from "@/api/generated/phone-verification-controller/phone-verification-controller";
import { authHeaders } from "@/auth/server";
import { normalizePhone } from "@/lib/phone";

export type PhoneState = { errors?: string[]; sentTo?: string; verified?: boolean; input?: string };

/** OTP (ADR-005): 6 haneli kod, 5 dk geçerli; 10 dk'da en fazla 3 kod, kod başına 5 deneme. */
export async function requestPhoneCodeAction(_: PhoneState, form: FormData): Promise<PhoneState> {
    const input = String(form.get("phone") ?? "");
    const phone = normalizePhone(input);
    if (!phone) return { errors: ["Numarayı 0532 123 45 67 biçiminde yaz."], input };
    try {
        await requestCode({ phone }, { headers: await authHeaders() });
    } catch (error) {
        return { errors: describeError(error), input };
    }
    return { sentTo: phone };
}

export async function confirmPhoneCodeAction(
    sentTo: string,
    _: PhoneState,
    form: FormData,
): Promise<PhoneState> {
    try {
        await confirmCode(
            { code: String(form.get("code") ?? "").trim() },
            { headers: await authHeaders() },
        );
    } catch (error) {
        return { sentTo, errors: describeError(error) };
    }
    refresh();
    return { verified: true };
}
