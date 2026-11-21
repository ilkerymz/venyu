"use client";

import { useActionState } from "react";
import { FormErrors, SubmitButton } from "@/components/form-parts";
import { type EmailState, resendEmailVerificationAction } from "./email-actions";

/** Doğrulanmamış e-posta: "Doğrula" yeni bağlantı gönderir. */
export function EmailRow() {
    const [state, action] = useActionState<EmailState>(resendEmailVerificationAction, {});
    if (state.sent) {
        return (
            <p role="status" className="mt-1 text-sm text-success">
                Yeni doğrulama bağlantısını gönderdik. E-postandaki bağlantıya tıkla; 24 saat
                geçerli.
            </p>
        );
    }
    return (
        <form action={action} className="mt-1 grid gap-2">
            <div className="flex items-center justify-between gap-3">
                <span className="text-sm text-muted-foreground">Doğrulanmadı</span>
                <SubmitButton
                    size="sm"
                    variant="outline"
                    className="h-8"
                    pendingText="Gönderiliyor…"
                    aria-label="E-postayı doğrula"
                >
                    Doğrula
                </SubmitButton>
            </div>
            <FormErrors errors={state.errors} />
        </form>
    );
}
