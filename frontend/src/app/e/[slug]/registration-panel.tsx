"use client";

import Link from "next/link";
import { startTransition, useActionState, useState } from "react";
import type { FormField, PublicTicket } from "@/api/generated/model";
import { Field, FormErrors, Notice } from "@/components/form-parts";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { readAnswers } from "@/lib/form-answers";
import { visibleFieldKeys } from "@/lib/form-visibility";
import { formatPrice } from "@/lib/format";
import { cn } from "@/lib/utils";
import { registerAction } from "./actions";

type Props = {
    eventId: string;
    tickets: PublicTicket[];
    fields: FormField[];
    isMember: boolean;
    requiresAccount: boolean;
    loginHref: string;
};

/** Ücretli bilet satışı ödeme altyapısıyla (F3) açılacak; şimdilik sadece ücretsiz biletler seçilebilir. */
const selectable = (t: PublicTicket) => !t.soldOut && (t.price ?? 0) === 0;

export function RegistrationPanel({
    eventId,
    tickets,
    fields,
    isMember,
    requiresAccount,
    loginHref,
}: Props) {
    const [state, dispatch, pending] = useActionState(registerAction.bind(null, eventId), {});
    const open = tickets.filter(selectable);
    const [ticketTypeId, setTicketTypeId] = useState(open.length === 1 ? (open[0].id ?? "") : "");
    const [answers, setAnswers] = useState<Record<string, unknown>>({});
    const visible = visibleFieldKeys(fields, answers, ticketTypeId);

    if (state.guestEmail) {
        return (
            <Notice>
                Neredeyse tamam. <strong>{state.guestEmail}</strong> adresine bir onay bağlantısı
                gönderdik; yerin 30 dakika boyunca ayrıldı. Bağlantıya tıklayınca biletin hazır
                olur.
            </Notice>
        );
    }

    if (requiresAccount && !isMember) {
        return (
            <div className="grid gap-3">
                <TicketList tickets={tickets} />
                <p className="text-sm text-muted-foreground">
                    Bu etkinliğe katılmak için Venyu hesabı gerekiyor.
                </p>
                <Button size="lg" asChild>
                    <Link href={loginHref}>Giriş yap ve kayıt ol</Link>
                </Button>
            </div>
        );
    }

    if (open.length === 0) {
        return (
            <div className="grid gap-3">
                <TicketList tickets={tickets} />
                <p className="text-sm text-muted-foreground">
                    {tickets.every((t) => t.soldOut)
                        ? "Bu etkinlik için bilet kalmadı."
                        : "Ücretli biletlerin satışı çok yakında açılıyor."}
                </p>
            </div>
        );
    }

    return (
        <form
            className="grid gap-5"
            onChange={(e) => {
                const data = new FormData(e.currentTarget);
                setTicketTypeId(String(data.get("ticketTypeId") ?? ""));
                setAnswers(readAnswers(fields, data));
            }}
            onSubmit={(e) => {
                // Form action yerine elle gönderim: React başarısız gönderimden sonra formu sıfırlamasın.
                e.preventDefault();
                const data = new FormData(e.currentTarget);
                startTransition(() => dispatch(data));
            }}
        >
            <fieldset className="grid gap-2">
                <legend className="mb-2 text-sm font-medium">Bilet</legend>
                {tickets.map((ticket) => (
                    <label
                        key={ticket.id}
                        className={cn(
                            "flex items-start gap-3 rounded-lg border border-input p-3",
                            selectable(ticket)
                                ? "cursor-pointer has-[:checked]:border-primary has-[:checked]:bg-secondary/50"
                                : "opacity-60",
                        )}
                    >
                        <input
                            type="radio"
                            name="ticketTypeId"
                            value={ticket.id}
                            required
                            disabled={!selectable(ticket)}
                            defaultChecked={ticket.id === ticketTypeId}
                            className="mt-1 size-4 accent-primary"
                        />
                        <span className="min-w-0 flex-1">
                            <span className="flex items-baseline justify-between gap-3">
                                <span className="font-medium">{ticket.name}</span>
                                <span className="shrink-0 text-sm font-semibold nums">
                                    {ticket.soldOut
                                        ? "Tükendi"
                                        : formatPrice(ticket.price ?? 0, ticket.currency ?? "TRY")}
                                </span>
                            </span>
                            {ticket.description && (
                                <span className="mt-0.5 block text-sm text-muted-foreground">
                                    {ticket.description}
                                </span>
                            )}
                            {!ticket.soldOut && (ticket.price ?? 0) > 0 && (
                                <span className="mt-0.5 block text-sm text-muted-foreground">
                                    Satış yakında açılıyor.
                                </span>
                            )}
                        </span>
                    </label>
                ))}
            </fieldset>

            {fields
                .filter((f) => visible.has(f.key))
                .map((field) => (
                    <QuestionField key={field.key} field={field} />
                ))}

            {!isMember && (
                <div className="grid gap-4 border-t pt-5">
                    <Field
                        label="Ad soyad"
                        name="guestName"
                        autoComplete="name"
                        maxLength={100}
                        required
                    />
                    <Field
                        label="E-posta"
                        name="guestEmail"
                        type="email"
                        autoComplete="email"
                        maxLength={254}
                        required
                        hint="Onay bağlantısı ve biletin bu adrese gelir."
                    />
                    <p className="text-sm text-muted-foreground">
                        Hesabın var mı?{" "}
                        <Link
                            href={loginHref}
                            className="font-medium text-primary underline-offset-4 hover:underline"
                        >
                            Giriş yap
                        </Link>
                    </p>
                </div>
            )}

            <FormErrors errors={state.errors} />
            <Button type="submit" size="lg" className="h-11" disabled={pending}>
                {pending ? "Kaydediliyor…" : "Kayıt ol"}
            </Button>
        </form>
    );
}

function TicketList({ tickets }: { tickets: PublicTicket[] }) {
    return (
        <ul className="divide-y">
            {tickets.map((t) => (
                <li key={t.id} className="flex items-baseline justify-between gap-3 py-3">
                    <span className={cn("font-medium", t.soldOut && "text-muted-foreground")}>
                        {t.name}
                    </span>
                    <span className="text-sm font-semibold nums">
                        {t.soldOut ? "Tükendi" : formatPrice(t.price ?? 0, t.currency ?? "TRY")}
                    </span>
                </li>
            ))}
        </ul>
    );
}

function QuestionField({ field }: { field: FormField }) {
    const id = `q-${field.key}`;
    // Label bileşeni flex; etiket ve "(isteğe bağlı)" tek parça kalsın diye tek span içinde.
    const label = (
        <span>
            {field.label}
            {!field.required && (
                <span className="font-normal text-muted-foreground"> (isteğe bağlı)</span>
            )}
        </span>
    );
    const options = field.options ?? [];

    switch (field.type) {
        case "LONG_TEXT":
            return (
                <div className="grid gap-2">
                    <Label htmlFor={id}>{label}</Label>
                    <Textarea
                        id={id}
                        name={field.key}
                        required={field.required}
                        maxLength={5000}
                        className="min-h-24 bg-card"
                    />
                </div>
            );
        case "BOOLEAN":
            return (
                <label className="flex cursor-pointer items-start gap-3 text-sm font-medium">
                    <input
                        type="checkbox"
                        name={field.key}
                        required={field.required}
                        className="mt-0.5 size-4 accent-primary"
                    />
                    <span>{label}</span>
                </label>
            );
        case "SINGLE_CHOICE":
        case "MULTI_CHOICE": {
            const multi = field.type === "MULTI_CHOICE";
            return (
                <fieldset className="grid gap-2">
                    <legend className="mb-1 text-sm font-medium">{label}</legend>
                    {options.map((option) => (
                        <label
                            key={option}
                            className="flex cursor-pointer items-center gap-3 text-sm"
                        >
                            <input
                                type={multi ? "checkbox" : "radio"}
                                name={field.key}
                                value={option}
                                required={field.required && !multi}
                                className="size-4 accent-primary"
                            />
                            {option}
                        </label>
                    ))}
                </fieldset>
            );
        }
        default:
            return (
                <Field
                    label={field.label + (field.required ? "" : " (isteğe bağlı)")}
                    id={id}
                    name={field.key}
                    type={field.type === "NUMBER" ? "number" : "text"}
                    inputMode={field.type === "NUMBER" ? "decimal" : undefined}
                    step={field.type === "NUMBER" ? "any" : undefined}
                    maxLength={field.type === "TEXT" ? 500 : undefined}
                    required={field.required}
                />
            );
    }
}
