"use server";

import { refresh } from "next/cache";
import { isApiError, describeError } from "@/api/api-error";
import { resendEmailVerification } from "@/api/generated/me-controller/me-controller";
import { authHeaders } from "@/auth/server";

export type EmailState = { errors?: string[]; sent?: boolean };

/** Yeni doğrulama bağlantısı (eskiler geçersiz olur); backend dakikada bir izin verir. */
export async function resendEmailVerificationAction(): Promise<EmailState> {
    try {
        await resendEmailVerification({ headers: await authHeaders() });
    } catch (error) {
        // Başka sekmede doğrulandıysa kart güncellensin.
        if (isApiError(error, 409)) refresh();
        return { errors: describeError(error) };
    }
    return { sent: true };
}
