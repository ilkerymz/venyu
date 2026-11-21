"use client";

import { useActionState, useState } from "react";
import { FormErrors } from "@/components/form-parts";
import { TicketPass } from "@/components/ticket-pass";
import { Button } from "@/components/ui/button";
import { cancelMyRegistrationAction } from "./actions";

type Props = { registrationId: string; ticketCode?: string; status?: string; ticketName?: string };

export function MyTicket({ registrationId, ticketCode, status, ticketName }: Props) {
    const [confirming, setConfirming] = useState(false);
    const [state, action, pending] = useActionState(
        cancelMyRegistrationAction.bind(null, registrationId),
        {},
    );
    return (
        <div className="grid gap-5">
            <p className="text-sm text-muted-foreground">Kayıtlısın. Kapıda bu QR kodunu göster.</p>
            <TicketPass code={ticketCode} status={status} title={ticketName} />
            <FormErrors errors={state.errors} />
            {status === "CONFIRMED" &&
                (confirming ? (
                    <form action={action} className="grid gap-2">
                        <p className="text-sm">Kaydını iptal edersen yerin başkasına açılır.</p>
                        <div className="flex gap-2">
                            <Button type="submit" variant="destructive" disabled={pending}>
                                {pending ? "İptal ediliyor…" : "Evet, iptal et"}
                            </Button>
                            <Button
                                type="button"
                                variant="ghost"
                                onClick={() => setConfirming(false)}
                            >
                                Vazgeç
                            </Button>
                        </div>
                    </form>
                ) : (
                    <Button
                        variant="ghost"
                        className="justify-self-center text-muted-foreground"
                        onClick={() => setConfirming(true)}
                    >
                        Kaydımı iptal et
                    </Button>
                ))}
        </div>
    );
}
