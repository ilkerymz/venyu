"use client";

import Link from "next/link";
import { useActionState, useState } from "react";
import type { SlotResponse } from "@/api/generated/model";
import { FormErrors, Notice, SubmitButton } from "@/components/form-parts";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { formatEventDay, formatTimeRange } from "@/lib/format";
import { groupByDay } from "@/lib/slots";
import { holdSlotAction } from "../actions";

export type Visitor = "guest" | "member" | "organizer";

type Props = { venueId: string; venueName: string; slots: SlotResponse[]; visitor: Visitor };

export function SlotPicker({ venueId, venueName, slots, visitor }: Props) {
    const [selected, setSelected] = useState<string>();
    const [sentFor, setSentFor] = useState<string>();
    const days = groupByDay(slots);

    if (sentFor) {
        return (
            <div className="mt-4 grid gap-3">
                <Notice>
                    Talebin {venueName}&apos;ye iletildi. Saat 24 saat boyunca senin için tutuluyor;
                    mekan onaylayınca etkinliğini oluşturabilirsin.
                </Notice>
                <Button asChild variant="outline" className="justify-self-start">
                    <Link href="/panel">Rezervasyonlarıma git</Link>
                </Button>
            </div>
        );
    }
    if (days.length === 0) {
        return (
            <p className="mt-3 text-sm text-muted-foreground">Önümüzdeki 60 günde boş saat yok.</p>
        );
    }

    return (
        <div className="mt-4 grid gap-5">
            {visitor !== "organizer" && (
                <p className="text-sm text-muted-foreground">
                    {visitor === "guest" ? (
                        <>
                            Saat ayırmak için{" "}
                            <Link
                                href={`/giris?next=/mekanlar/${venueId}`}
                                className="font-medium text-primary underline-offset-4 hover:underline"
                            >
                                giriş yap
                            </Link>
                            .
                        </>
                    ) : (
                        <>
                            Saat ayırmak için{" "}
                            <Link
                                href="/hesabim"
                                className="font-medium text-primary underline-offset-4 hover:underline"
                            >
                                organizatör ol
                            </Link>
                            .
                        </>
                    )}
                </p>
            )}
            {days.map(([day, items]) => (
                <div key={day}>
                    <h3 className="mb-2 text-sm font-medium text-muted-foreground">
                        {formatEventDay(items[0].startsAt ?? "")}
                    </h3>
                    <ul className="grid gap-2">
                        {items.map((slot) => (
                            <li key={slot.id} className="rounded-lg border">
                                <div className="flex items-center justify-between gap-3 px-3 py-2">
                                    <span className="font-medium nums">
                                        {formatTimeRange(slot.startsAt ?? "", slot.endsAt ?? "")}
                                    </span>
                                    {visitor === "organizer" && selected !== slot.id && (
                                        <Button
                                            size="sm"
                                            variant="outline"
                                            onClick={() => setSelected(slot.id)}
                                        >
                                            Bu saati ayır
                                        </Button>
                                    )}
                                </div>
                                {selected === slot.id && (
                                    <HoldForm
                                        venueId={venueId}
                                        slotId={slot.id ?? ""}
                                        onCancel={() => setSelected(undefined)}
                                        onDone={() => setSentFor(slot.id)}
                                    />
                                )}
                            </li>
                        ))}
                    </ul>
                </div>
            ))}
        </div>
    );
}

function HoldForm({
    venueId,
    slotId,
    onCancel,
    onDone,
}: {
    venueId: string;
    slotId: string;
    onCancel: () => void;
    onDone: () => void;
}) {
    const [state, action] = useActionState(
        async (prev: Parameters<typeof holdSlotAction>[2], form: FormData) => {
            const result = await holdSlotAction(venueId, slotId, prev, form);
            if (result.done) onDone();
            return result;
        },
        {},
    );
    return (
        <form action={action} className="grid gap-3 border-t p-3">
            <FormErrors errors={state.errors} />
            <div className="grid gap-2">
                <Label htmlFor={`note-${slotId}`}>Mekana not</Label>
                <Textarea
                    id={`note-${slotId}`}
                    name="note"
                    maxLength={500}
                    defaultValue={state.values?.note}
                    placeholder="Ne tür bir etkinlik, kaç kişi, özel bir ihtiyacın var mı?"
                    className="min-h-20 bg-card"
                />
            </div>
            <div className="flex gap-2">
                <SubmitButton pendingText="Gönderiliyor…" size="default" className="h-9">
                    Talep gönder
                </SubmitButton>
                <Button type="button" variant="ghost" onClick={onCancel}>
                    Vazgeç
                </Button>
            </div>
        </form>
    );
}
