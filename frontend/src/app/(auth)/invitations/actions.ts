"use server";

import { describeError } from "@/api/api-error";
import {
    accept,
    acceptAsGuest,
    preview,
} from "@/api/generated/invitation-controller/invitation-controller";
import { authHeaders } from "@/auth/server";
import type { RegisterState } from "@/components/registration-panel";
import { labelErrors, readAnswers } from "@/lib/form-answers";

/**
 * Davet kabulü (ADR-008): davet tek kullanımlık; davetli misafir e-posta onayı beklemeden CONFIRMED.
 * Oturum varsa üye olarak, yoksa sadece adla misafir olarak kabul edilir (e-posta davetten gelir).
 */
export async function acceptInvitationAction(
    token: string,
    _: RegisterState,
    form: FormData,
): Promise<RegisterState> {
    const fields = await preview({ token })
        .then((r) => r.data.formFields ?? [])
        .catch(() => []);
    const ticketTypeId = String(form.get("ticketTypeId") ?? "");
    const answers = readAnswers(fields, form);
    const headers = await authHeaders();

    try {
        const { data } =
            "authorization" in headers
                ? await accept({ token, ticketTypeId, answers }, { headers })
                : await acceptAsGuest({
                      token,
                      ticketTypeId,
                      answers,
                      name: String(form.get("guestName") ?? "").trim(),
                  });
        return { ticketCode: data.ticketCode };
    } catch (error) {
        return { errors: labelErrors(describeError(error), fields) };
    }
}
