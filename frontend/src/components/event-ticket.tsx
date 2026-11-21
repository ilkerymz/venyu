import { Clock, MapPin } from "lucide-react";
import { ThresholdMeter } from "@/components/threshold-meter";
import { eventDateParts, formatPrice, formatTimeRange } from "@/lib/format";
import { cn } from "@/lib/utils";

type EventTicketProps = {
    title: string;
    place: string;
    startsAt: string;
    endsAt: string;
    price: number;
    currency: string;
    /** Minimum katılımcı garantisi varsa eşik göstergesi çizilir. */
    threshold?: { minimum: number; current: number };
    className?: string;
};

/**
 * Venyu'nun imza bileşeni: etkinliği bir bilet koçanı olarak gösterir.
 * Sol koçan tarihi, sağ gövde etkinliği taşır; arada delikli yırtma çizgisi.
 */
export function EventTicket({
    title,
    place,
    startsAt,
    endsAt,
    price,
    currency,
    threshold,
    className,
}: EventTicketProps) {
    const date = eventDateParts(startsAt);

    return (
        <article className={cn("drop-shadow-[0_18px_30px_rgb(21_23_46/0.14)]", className)}>
            <div className="flex overflow-hidden rounded-xl bg-card ticket-cut">
                <div className="flex w-20 shrink-0 flex-col items-center justify-center bg-primary py-5 text-primary-foreground sm:w-24">
                    <span className="font-display text-4xl leading-none font-bold nums sm:text-5xl">
                        {date.day}
                    </span>
                    <span className="mt-1 text-sm font-medium">{date.month}</span>
                    <span className="text-xs text-primary-foreground/70">{date.weekday}</span>
                </div>

                <div className="min-w-0 flex-1 border-l-2 border-dashed border-border p-5 sm:p-6">
                    <div className="flex items-start justify-between gap-4">
                        <h3 className="text-xl leading-tight font-semibold sm:text-2xl">{title}</h3>
                        <span className="shrink-0 pt-0.5 text-base font-semibold nums">
                            {formatPrice(price, currency)}
                        </span>
                    </div>

                    <dl className="mt-3 space-y-1.5 text-sm text-muted-foreground">
                        <div className="flex items-center gap-2">
                            <dt className="sr-only">Yer</dt>
                            <MapPin className="size-4" aria-hidden />
                            <dd className="truncate">{place}</dd>
                        </div>
                        <div className="flex items-center gap-2">
                            <dt className="sr-only">Saat</dt>
                            <Clock className="size-4" aria-hidden />
                            <dd className="nums">{formatTimeRange(startsAt, endsAt)}</dd>
                        </div>
                    </dl>

                    {threshold && <ThresholdMeter className="mt-5" {...threshold} />}
                </div>
            </div>
        </article>
    );
}
