"use client";

import { useActionState } from "react";
import { requestPasswordResetAction } from "@/auth/actions";
import { Field, FormErrors, Notice, SubmitButton } from "@/components/form-parts";

export function ForgotPasswordForm() {
    const [state, action] = useActionState(requestPasswordResetAction, {});
    if (state.done) {
        return (
            <Notice>
                Bu e-postayla bir hesap varsa şifre sıfırlama bağlantısını gönderdik. Gelen kutunu
                kontrol et; bağlantı bir süre sonra geçersiz olur.
            </Notice>
        );
    }
    return (
        <form action={action} className="grid gap-5">
            <FormErrors errors={state.errors} />
            <Field
                label="E-posta"
                name="email"
                defaultValue={state.values?.email}
                type="email"
                autoComplete="email"
                required
            />
            <SubmitButton pendingText="Gönderiliyor…">Sıfırlama bağlantısı gönder</SubmitButton>
        </form>
    );
}
