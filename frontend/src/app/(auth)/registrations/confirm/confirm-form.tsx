"use client";

import { useActionState } from "react";
import { FormErrors, Notice, SubmitButton } from "@/components/form-parts";
import { TicketPass } from "@/components/ticket-pass";
import { confirmRegistrationAction } from "../actions";

export function ConfirmRegistrationForm({ token }: { token: string }) {
    const [state, action] = useActionState(confirmRegistrationAction, {});
    if (state.ticketCode) {
        return (
            <div className="grid gap-6">
                <Notice>
                    Kaydın onaylandı. Biletini ve kaydını yönetme bağlantısını e-postana da
                    gönderdik.
                </Notice>
                <div className="rounded-xl border bg-card p-6">
                    <TicketPass code={state.ticketCode} status="CONFIRMED" />
                </div>
                <p className="text-sm text-muted-foreground">
                    Kapıda bu QR kodunu göstermen yeterli.
                </p>
            </div>
        );
    }
    return (
        <form action={action} className="grid gap-5">
            <FormErrors errors={state.errors} />
            <input type="hidden" name="token" value={token} />
            <SubmitButton pendingText="Onaylanıyor…" className="justify-self-start">
                Kaydımı onayla
            </SubmitButton>
        </form>
    );
}
