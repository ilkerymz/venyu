import { MapPin } from "lucide-react";
import Link from "next/link";
import { useId } from "react";
import { eventDateParts, formatEventDay, formatTimeRange } from "@/lib/format";
import { cn } from "@/lib/utils";

type Props = {
    href: string;
    title: string;
    startsAt: string;
    endsAt: string;
    place?: string;
    /** Kartın altındaki durum (etkinlik durumu, bilet durumu…). */
    badge?: React.ReactNode;
    /** Geçmiş veya iptal edilmiş etkinlik: koçan sönük çizilir. */
    muted?: boolean;
};

/**
 * Listelerde etkinlik kartı: `EventTicket`'ın küçük kardeşi. Sol koçan tarihi taşır,
 * kartın tamamı etkinlik sayfasına bağlantıdır.
 */
export function EventCard({ href, title, startsAt, endsAt, place, badge, muted }: Props) {
    const date = eventDateParts(startsAt);
    const id = useId();
    // Bağlantının adı başlık, tarih/yer/durum açıklaması (ekran okuyucu koçanı iki kez okumasın).
    return (
        <Link
            href={href}
            aria-labelledby={`${id}-title`}
            aria-describedby={`${id}-meta ${id}-status`}
            className="group block h-full rounded-xl outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50"
        >
            <div className="h-full drop-shadow-[0_6px_14px_rgb(21_23_46/0.08)] transition-[filter] group-hover:drop-shadow-[0_14px_26px_rgb(21_23_46/0.14)]">
                <div className="flex h-full overflow-hidden rounded-xl bg-card ticket-cut">
                    <div
                        aria-hidden
                        className={cn(
                            "flex w-20 shrink-0 flex-col items-center justify-center py-4 sm:w-24",
                            muted
                                ? "bg-muted text-muted-foreground"
                                : "bg-primary text-primary-foreground",
                        )}
                    >
                        <span className="font-display text-3xl leading-none font-bold nums">
                            {date.day}
                        </span>
                        <span className="mt-1 text-sm font-medium">{date.month}</span>
                        <span className="text-xs opacity-70">{date.weekday}</span>
                    </div>
                    <div className="flex min-w-0 flex-1 flex-col border-l-2 border-dashed border-border p-4 sm:p-5">
                        <h3
                            id={`${id}-title`}
                            className="line-clamp-2 leading-snug font-semibold underline-offset-4 group-hover:underline"
                        >
                            {title}
                        </h3>
                        <div id={`${id}-meta`}>
                            <p className="mt-1.5 text-sm text-muted-foreground nums">
                                {formatEventDay(startsAt)}, {formatTimeRange(startsAt, endsAt)}
                            </p>
                            {place && (
                                <p className="mt-0.5 flex items-center gap-1.5 text-sm text-muted-foreground">
                                    <MapPin className="size-3.5 shrink-0" aria-hidden />
                                    <span className="truncate">{place}</span>
                                </p>
                            )}
                        </div>
                        {badge && (
                            <div id={`${id}-status`} className="mt-auto pt-3">
                                {badge}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </Link>
    );
}
