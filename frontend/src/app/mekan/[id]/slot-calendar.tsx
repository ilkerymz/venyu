"use client";

import { useActionState, useState, useSyncExternalStore, useTransition } from "react";
import type { BookingResponse, SlotResponse } from "@/api/generated/model";
import type { FormState } from "@/auth/actions";
import { Field, FormErrors, Notice, SubmitButton } from "@/components/form-parts";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { formatEventDay, formatTimeRange } from "@/lib/format";
import { groupByDay } from "@/lib/slots";
import { closeSlotAction, openSlotAction } from "../actions";

const noop = () => () => {};
const istanbulToday = () =>
    new Intl.DateTimeFormat("en-CA", { timeZone: "Europe/Istanbul" }).format(new Date());

type Props = {
    venueId: string;
    slots: SlotResponse[];
    activeBookings: Record<string, BookingResponse>;
};

export function SlotCalendar({ venueId, slots, activeBookings }: Props) {
    const [state, action] = useActionState(openSlotAction.bind(null, venueId), {});
    const today = useSyncExternalStore(noop, istanbulToday, () => undefined);
    const v = state.values ?? {};
    const days = groupByDay(slots);

    return (
        <div className="grid gap-10 lg:grid-cols-[20rem_1fr]">
            <section className="self-start rounded-xl border bg-card p-5">
                <h2 className="text-lg font-semibold">Boş saat aç</h2>
                <p className="mt-1 text-sm text-muted-foreground">
                    Organizatörler bu saatleri etkinlikleri için ayırabilir; talepleri sen
                    onaylarsın.
                </p>
                <form action={action} className="mt-4 grid gap-4">
                    <FormErrors errors={state.errors} />
                    {state.done && <Notice>Saat takvime eklendi.</Notice>}
                    <Field
                        label="Tarih"
                        name="date"
                        type="date"
                        min={today}
                        required
                        defaultValue={v.date}
                    />
                    <div className="grid grid-cols-2 gap-3">
                        <Field
                            label="Başlangıç"
                            name="startTime"
                            type="time"
                            required
                            defaultValue={v.startTime ?? "19:00"}
                        />
                        <Field
                            label="Bitiş"
                            name="endTime"
                            type="time"
                            required
                            defaultValue={v.endTime ?? "23:00"}
                        />
                    </div>
                    <p className="-mt-1 text-xs text-muted-foreground">
                        Bitiş başlangıçtan önceyse ertesi gün sayılır.
                    </p>
                    <SubmitButton pendingText="Ekleniyor…">Takvime ekle</SubmitButton>
                </form>
            </section>

            <section aria-labelledby="slots-heading">
                <h2 id="slots-heading" className="text-lg font-semibold">
                    Önümüzdeki 60 gün
                </h2>
                {days.length === 0 ? (
                    <p className="mt-3 text-muted-foreground">
                        Henüz açık saatin yok. Soldan ilk boş saatini ekle; örneğin hafta içi
                        akşamları.
                    </p>
                ) : (
                    <div className="mt-4 grid gap-6">
                        {days.map(([day, items]) => (
                            <div key={day}>
                                <h3 className="mb-2 text-sm font-medium text-muted-foreground">
                                    {formatEventDay(items[0].startsAt ?? "")}
                                </h3>
                                <ul className="divide-y rounded-xl border bg-card">
                                    {items.map((slot) => (
                                        <SlotRow
                                            key={slot.id}
                                            venueId={venueId}
                                            slot={slot}
                                            booking={activeBookings[slot.id ?? ""]}
                                        />
                                    ))}
                                </ul>
                            </div>
                        ))}
                    </div>
                )}
            </section>
        </div>
    );
}

function SlotRow({
    venueId,
    slot,
    booking,
}: {
    venueId: string;
    slot: SlotResponse;
    booking?: BookingResponse;
}) {
    const [pending, startTransition] = useTransition();
    const [result, setResult] = useState<FormState>({});
    return (
        <li className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
            <span className="font-medium nums">
                {formatTimeRange(slot.startsAt ?? "", slot.endsAt ?? "")}
            </span>
            <span className="flex items-center gap-3">
                {booking?.status === "CONFIRMED" ? (
                    <Badge className="bg-success-soft text-success">Rezerve</Badge>
                ) : booking?.status === "HELD" ? (
                    <Badge className="bg-secondary text-secondary-foreground">Onay bekliyor</Badge>
                ) : (
                    <Badge variant="outline">Boş</Badge>
                )}
                {!booking && (
                    <Button
                        variant="ghost"
                        size="sm"
                        className="text-destructive"
                        disabled={pending}
                        onClick={() =>
                            startTransition(async () =>
                                setResult(await closeSlotAction(venueId, slot.id ?? "")),
                            )
                        }
                    >
                        {pending ? "Kaldırılıyor…" : "Kaldır"}
                    </Button>
                )}
            </span>
            {result.errors && (
                <div className="w-full">
                    <FormErrors errors={result.errors} />
                </div>
            )}
        </li>
    );
}
