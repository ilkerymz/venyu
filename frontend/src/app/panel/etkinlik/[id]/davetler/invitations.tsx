"use client";

import { useState, useTransition } from "react";
import type { InvitationResponse } from "@/api/generated/model";
import { FormErrors, Notice } from "@/components/form-parts";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import type { FormState } from "@/auth/actions";
import { parseEmails } from "@/lib/emails";
import { inviteAction } from "../../../actions";

const MAX_PER_SEND = 100;
const date = new Intl.DateTimeFormat("tr-TR", {
    timeZone: "Europe/Istanbul",
    day: "numeric",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
});

type Props = {
    eventId: string;
    invitations: InvitationResponse[];
    editable: boolean;
    visibility?: string;
};

export function Invitations({ eventId, invitations, editable, visibility }: Props) {
    const [text, setText] = useState("");
    const [state, setState] = useState<FormState>({});
    const [pending, startTransition] = useTransition();
    const { valid, invalid } = parseEmails(text);
    const accepted = invitations.filter((i) => i.status === "ACCEPTED").length;

    function send() {
        startTransition(async () => {
            const result = await inviteAction(eventId, valid);
            setState(result);
            if (result.done) setText("");
        });
    }

    return (
        <div className="grid max-w-3xl gap-8">
            {editable && (
                <section className="grid gap-4 rounded-xl border bg-card p-6">
                    <h2 className="text-xl font-semibold">Davet gönder</h2>
                    {visibility !== "INVITE_ONLY" && (
                        <p className="text-sm text-muted-foreground">
                            Davetliler kayıt olurken e-posta onayı beklemez. Etkinliği sadece
                            davetlilere açmak istersen Genel sekmesinden görünürlüğü &quot;Sadece
                            davetliler&quot; yap.
                        </p>
                    )}
                    <div className="grid gap-2">
                        <Label htmlFor="emails">E-posta adresleri</Label>
                        <Textarea
                            id="emails"
                            value={text}
                            onChange={(e) => {
                                setText(e.target.value);
                                setState({});
                            }}
                            placeholder={
                                "Her satıra bir adres ya da virgülle ayır\nayse@ornek.com, can@ornek.com"
                            }
                            className="min-h-32 bg-card"
                        />
                        {text.trim() && (
                            <p className="text-sm text-muted-foreground" aria-live="polite">
                                {valid.length} adres gönderilecek.
                                {invalid.length > 0 && (
                                    <span className="text-destructive">
                                        {" "}
                                        Hatalı görünenler: {invalid.join(", ")}
                                    </span>
                                )}
                            </p>
                        )}
                    </div>
                    <FormErrors
                        errors={
                            valid.length > MAX_PER_SEND
                                ? [
                                      `Bir seferde en fazla ${MAX_PER_SEND} adrese davet gönderebilirsin.`,
                                  ]
                                : state.errors
                        }
                    />
                    {state.done && (
                        <Notice>
                            Davetler gönderildi. Aynı adrese tekrar göndermek yeni e-posta
                            göndermez.
                        </Notice>
                    )}
                    <Button
                        className="justify-self-start"
                        onClick={send}
                        disabled={pending || valid.length === 0 || valid.length > MAX_PER_SEND}
                    >
                        {pending
                            ? "Gönderiliyor…"
                            : valid.length > 1
                              ? `${valid.length} davet gönder`
                              : "Davet gönder"}
                    </Button>
                </section>
            )}

            <section className="grid gap-4">
                <h2 className="text-xl font-semibold">
                    Gönderilen davetler{" "}
                    <span className="font-normal text-muted-foreground nums">
                        {invitations.length > 0 && `(${accepted}/${invitations.length} kabul etti)`}
                    </span>
                </h2>
                {invitations.length === 0 ? (
                    <p className="text-muted-foreground">Henüz davet gönderilmedi.</p>
                ) : (
                    <ul className="divide-y rounded-xl border bg-card">
                        {invitations.map((inv) => (
                            <li
                                key={inv.id}
                                className="flex flex-wrap items-center justify-between gap-3 px-4 py-3"
                            >
                                <span className="min-w-0 truncate">{inv.email}</span>
                                <span className="flex items-center gap-3">
                                    <span className="text-sm text-muted-foreground nums">
                                        {date.format(
                                            new Date(inv.acceptedAt ?? inv.createdAt ?? ""),
                                        )}
                                    </span>
                                    {inv.status === "ACCEPTED" ? (
                                        <Badge className="bg-success-soft text-success">
                                            Kabul etti
                                        </Badge>
                                    ) : (
                                        <Badge variant="secondary">Bekliyor</Badge>
                                    )}
                                </span>
                            </li>
                        ))}
                    </ul>
                )}
            </section>
        </div>
    );
}
