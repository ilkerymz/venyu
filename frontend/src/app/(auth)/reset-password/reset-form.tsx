"use client";

import { useActionState } from "react";
import { confirmPasswordResetAction } from "@/auth/actions";
import { Field, FormErrors, SubmitButton } from "@/components/form-parts";

export function ResetPasswordForm({ token }: { token: string }) {
    const [state, action] = useActionState(confirmPasswordResetAction, {});
    return (
        <form action={action} className="grid gap-5">
            <FormErrors errors={state.errors} />
            <input type="hidden" name="token" value={token} />
            <Field
                label="Yeni şifre"
                name="newPassword"
                type="password"
                autoComplete="new-password"
                minLength={8}
                maxLength={72}
                required
                hint="En az 8 karakter. Şifreyi yenilediğinde bütün cihazlardaki oturumların kapanır."
            />
            <SubmitButton pendingText="Kaydediliyor…">Şifreyi yenile</SubmitButton>
        </form>
    );
}
