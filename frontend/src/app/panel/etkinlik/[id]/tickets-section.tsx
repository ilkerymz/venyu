"use client";

import { useActionState, useEffect, useRef, useState } from "react";
import type { TicketTypeResponse } from "@/api/generated/model";
import { Field, FormErrors, SubmitButton } from "@/components/form-parts";
import { Button } from "@/components/ui/button";
import { formatPrice } from "@/lib/format";
import { addTicketTypeAction, deleteTicketTypeAction, updateTicketTypeAction } from "../../actions";

type Props = { eventId: string; tickets: TicketTypeResponse[]; editable: boolean };

export function TicketsSection({ eventId, tickets, editable }: Props) {
    const [adding, setAdding] = useState(false);
    return (
        <div className="grid gap-4">
            <ul className="divide-y rounded-lg border">
                {tickets.map((ticket) => (
                    <TicketRow
                        key={ticket.id}
                        eventId={eventId}
                        ticket={ticket}
                        editable={editable}
                    />
                ))}
            </ul>
            {editable &&
                (adding ? (
                    <TicketForm eventId={eventId} onDone={() => setAdding(false)} />
                ) : (
                    <Button
                        variant="outline"
                        className="justify-self-start"
                        onClick={() => setAdding(true)}
                    >
                        Bilet türü ekle
                    </Button>
                ))}
        </div>
    );
}

function TicketRow({
    eventId,
    ticket,
    editable,
}: {
    eventId: string;
    ticket: TicketTypeResponse;
    editable: boolean;
}) {
    const [editing, setEditing] = useState(false);
    const [state, remove] = useActionState(
        deleteTicketTypeAction.bind(null, eventId, ticket.id ?? ""),
        {},
    );
    const sold = ticket.sold ?? 0;

    if (editing) {
        return (
            <li className="p-4">
                <TicketForm eventId={eventId} ticket={ticket} onDone={() => setEditing(false)} />
            </li>
        );
    }
    return (
        <li className="grid gap-3 p-4 sm:grid-cols-[1fr_auto] sm:items-center">
            <div className="min-w-0">
                <p className="flex flex-wrap items-baseline gap-x-3 font-medium">
                    {ticket.name}{" "}
                    <span className="font-normal text-muted-foreground nums">
                        {formatPrice(ticket.price ?? 0, ticket.currency ?? "TRY")}
                    </span>
                </p>
                {ticket.description && (
                    <p className="mt-1 text-sm text-muted-foreground">{ticket.description}</p>
                )}
                <p className="mt-1 text-sm text-muted-foreground nums">
                    {ticket.capacity
                        ? `${sold}/${ticket.capacity} kayıt`
                        : `${sold} kayıt, kapasite sınırsız`}
                </p>
                <FormErrors errors={state.errors} />
            </div>
            {editable && (
                <div className="flex gap-2">
                    <Button variant="ghost" size="sm" onClick={() => setEditing(true)}>
                        Düzenle
                    </Button>
                    <form action={remove}>
                        <SubmitButton
                            variant="ghost"
                            size="sm"
                            className="h-8 text-destructive"
                            pendingText="Siliniyor…"
                            disabled={sold > 0}
                            title={sold > 0 ? "Kaydı olan bilet silinemez." : undefined}
                        >
                            Sil
                        </SubmitButton>
                    </form>
                </div>
            )}
        </li>
    );
}

function TicketForm({
    eventId,
    ticket,
    onDone,
}: {
    eventId: string;
    ticket?: TicketTypeResponse;
    onDone: () => void;
}) {
    const action = ticket
        ? updateTicketTypeAction.bind(null, eventId, ticket.id ?? "")
        : addTicketTypeAction.bind(null, eventId);
    const [state, formAction] = useActionState(action, {});
    const onDoneRef = useRef(onDone);
    useEffect(() => {
        if (state.done) onDoneRef.current();
    }, [state]);
    const v = state.values ?? {};
    const prefix = ticket ? `tt-${ticket.id}-` : "tt-new-";

    return (
        <form action={formAction} className="grid gap-4 rounded-lg bg-muted/40 p-4">
            <FormErrors errors={state.errors} />
            <div className="grid gap-4 sm:grid-cols-[1fr_8rem_8rem]">
                <Field
                    label="Bilet adı"
                    id={`${prefix}name`}
                    name="name"
                    maxLength={100}
                    required
                    defaultValue={v.name ?? ticket?.name}
                    placeholder="Örn. Standart"
                />
                <Field
                    label="Fiyat (₺)"
                    id={`${prefix}price`}
                    name="price"
                    type="number"
                    min={0}
                    step="0.01"
                    inputMode="decimal"
                    required
                    defaultValue={v.price ?? ticket?.price ?? 0}
                />
                <Field
                    label="Kapasite"
                    id={`${prefix}capacity`}
                    name="capacity"
                    type="number"
                    min={1}
                    max={100000}
                    placeholder="Sınırsız"
                    defaultValue={v.capacity ?? ticket?.capacity}
                />
            </div>
            <Field
                label="Açıklama"
                id={`${prefix}description`}
                name="description"
                maxLength={1000}
                placeholder="İsteğe bağlı, örn. İlk 10 kişiye"
                defaultValue={v.description ?? ticket?.description}
            />
            <p className="-mt-1 text-sm text-muted-foreground">
                Ücretli biletlerin satışı ödeme altyapısı gelince açılacak; şimdilik
                tanımlayabilirsin.
            </p>
            <div className="flex gap-2">
                <SubmitButton pendingText="Kaydediliyor…" size="default" className="h-9">
                    {ticket ? "Kaydet" : "Bileti ekle"}
                </SubmitButton>
                <Button type="button" variant="ghost" onClick={onDone}>
                    Vazgeç
                </Button>
            </div>
        </form>
    );
}
