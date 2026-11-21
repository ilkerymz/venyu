"use client";

import { useState, useTransition } from "react";
import type { BookingResponse, SlotResponse } from "@/api/generated/model";
import type { FormState } from "@/auth/actions";
import { FormErrors } from "@/components/form-parts";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { formatEventDay, formatTimeRange } from "@/lib/format";
import { decideBookingAction } from "../../actions";

export const BOOKING_STATUS: Record<string, { label: string; className: string }> = {
    HELD: { label: "Onay bekliyor", className: "bg-secondary text-secondary-foreground" },
    CONFIRMED: { label: "Onaylandı", className: "bg-success-soft text-success" },
    REJECTED: { label: "Reddedildi", className: "bg-muted text-muted-foreground" },
    EXPIRED: { label: "Süresi doldu", className: "bg-muted text-muted-foreground" },
    COMPLETED: { label: "Tamamlandı", className: "bg-muted text-foreground" },
};
const time = new Intl.DateTimeFormat("tr-TR", {
    timeZone: "Europe/Istanbul",
    day: "numeric",
    month: "long",
    hour: "2-digit",
    minute: "2-digit",
});
const ORDER = ["HELD", "CONFIRMED", "COMPLETED", "REJECTED", "EXPIRED"];

export function BookingList({
    bookings,
    slots,
}: {
    bookings: BookingResponse[];
    slots: Record<string, SlotResponse>;
}) {
    const sorted = [...bookings].sort(
        (a, b) =>
            ORDER.indexOf(a.status ?? "") - ORDER.indexOf(b.status ?? "") ||
            (slots[a.slotId ?? ""]?.startsAt ?? "").localeCompare(
                slots[b.slotId ?? ""]?.startsAt ?? "",
            ),
    );
    if (sorted.length === 0) {
        return (
            <p className="max-w-3xl rounded-xl border bg-card p-6 text-muted-foreground">
                Henüz rezervasyon talebi yok. Takvime boş saat ekledikçe organizatörler talep
                gönderebilir.
            </p>
        );
    }
    return (
        <ul className="grid max-w-3xl divide-y rounded-xl border bg-card">
            {sorted.map((b) => (
                <BookingRow key={b.id} booking={b} slot={slots[b.slotId ?? ""]} />
            ))}
        </ul>
    );
}

function BookingRow({ booking, slot }: { booking: BookingResponse; slot?: SlotResponse }) {
    const [pending, startTransition] = useTransition();
    const [result, setResult] = useState<FormState>({});
    const status = BOOKING_STATUS[booking.status ?? ""];
    const decide = (d: "confirm" | "reject") =>
        startTransition(async () => setResult(await decideBookingAction(booking.id ?? "", d)));

    return (
        <li className="grid gap-3 p-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <p className="font-semibold nums">
                    {slot
                        ? `${formatEventDay(slot.startsAt ?? "")}, ${formatTimeRange(slot.startsAt ?? "", slot.endsAt ?? "")}`
                        : "Geçmiş ya da kaldırılmış saat"}
                </p>
                {status && <Badge className={status.className}>{status.label}</Badge>}
            </div>
            {booking.note && (
                <p className="rounded-lg bg-muted/60 px-3 py-2 text-sm">“{booking.note}”</p>
            )}
            {booking.status === "HELD" && (
                <>
                    {booking.expiresAt && (
                        <p className="text-sm text-muted-foreground nums">
                            Yanıt vermezsen talep {time.format(new Date(booking.expiresAt))}{" "}
                            itibarıyla düşer ve saat yeniden boşalır.
                        </p>
                    )}
                    <FormErrors errors={result.errors} />
                    <div className="flex gap-2">
                        <Button disabled={pending} onClick={() => decide("confirm")}>
                            Onayla
                        </Button>
                        <Button
                            variant="outline"
                            disabled={pending}
                            onClick={() => decide("reject")}
                        >
                            Reddet
                        </Button>
                    </div>
                </>
            )}
        </li>
    );
}
