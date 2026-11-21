import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { formatEventDay, formatTimeRange } from "@/lib/format";
import type { BookingView } from "./data";

const STATUS: Record<string, { label: string; className: string }> = {
    HELD: {
        label: "Mekanın yanıtı bekleniyor",
        className: "bg-secondary text-secondary-foreground",
    },
    CONFIRMED: { label: "Onaylandı", className: "bg-success-soft text-success" },
    REJECTED: { label: "Reddedildi", className: "bg-muted text-muted-foreground" },
    EXPIRED: { label: "Süresi doldu", className: "bg-muted text-muted-foreground" },
    COMPLETED: { label: "Tamamlandı", className: "bg-muted text-foreground" },
};
const ORDER = ["CONFIRMED", "HELD", "COMPLETED", "REJECTED", "EXPIRED"];

/** Organizatörün mekan rezervasyonları: onaylanan rezervasyondan tek tıkla etkinlik oluşturulur. */
export function MyBookings({ bookings }: { bookings: BookingView[] }) {
    const sorted = [...bookings].sort(
        (a, b) =>
            ORDER.indexOf(a.booking.status ?? "") - ORDER.indexOf(b.booking.status ?? "") ||
            (a.slot?.startsAt ?? "").localeCompare(b.slot?.startsAt ?? ""),
    );
    return (
        <ul className="divide-y rounded-xl border bg-card">
            {sorted.map(({ booking, venue, slot, event }) => {
                const status = STATUS[booking.status ?? ""];
                return (
                    <li
                        key={booking.id}
                        className="flex flex-wrap items-center justify-between gap-3 p-4 sm:p-5"
                    >
                        <div className="min-w-0">
                            <p className="font-semibold">
                                {venue ? (
                                    <Link
                                        href={`/mekanlar/${venue.id}`}
                                        className="underline-offset-4 hover:underline"
                                    >
                                        {venue.name}
                                    </Link>
                                ) : (
                                    "Mekan"
                                )}
                                {venue?.district && (
                                    <span className="font-normal text-muted-foreground">
                                        , {venue.district}
                                    </span>
                                )}
                            </p>
                            <p className="mt-0.5 text-sm text-muted-foreground nums">
                                {slot
                                    ? `${formatEventDay(slot.startsAt ?? "")}, ${formatTimeRange(slot.startsAt ?? "", slot.endsAt ?? "")}`
                                    : "Saat bilgisi yok"}
                            </p>
                        </div>
                        <div className="flex flex-wrap items-center gap-3">
                            {status && <Badge className={status.className}>{status.label}</Badge>}
                            {booking.status === "CONFIRMED" &&
                                (event ? (
                                    <Button variant="ghost" size="sm" asChild>
                                        <Link href={`/panel/etkinlik/${event.id}`}>
                                            Etkinliğe git
                                        </Link>
                                    </Button>
                                ) : (
                                    <Button size="sm" asChild>
                                        <Link
                                            href={`/panel/etkinlik/yeni?rezervasyon=${booking.id}`}
                                        >
                                            Etkinlik oluştur
                                        </Link>
                                    </Button>
                                ))}
                        </div>
                    </li>
                );
            })}
        </ul>
    );
}
