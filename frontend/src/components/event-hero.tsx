import { MapPin } from "lucide-react";
import { eventDateParts, formatEventDay, formatTimeRange } from "@/lib/format";
import { cn } from "@/lib/utils";

type Props = {
    title: string;
    startsAt: string;
    endsAt: string;
    place?: string;
    className?: string;
};

/**
 * Etkinliğin bilet koçanı biçimindeki başlığı. Etkinlik sayfası ve organizatörün yönetim
 * sekmeleri aynı başlığı kullanır: sekmeler arasında gezerken sayfa yapısı değişmez.
 */
export function EventHero({ title, startsAt, endsAt, place, className }: Props) {
    const date = eventDateParts(startsAt);
    return (
        <div className={cn("drop-shadow-[0_18px_30px_rgb(21_23_46/0.12)]", className)}>
            <div className="flex overflow-hidden rounded-xl bg-card ticket-cut">
                <div className="flex w-20 shrink-0 flex-col items-center justify-center bg-primary py-6 text-primary-foreground sm:w-24">
                    <span className="font-display text-4xl leading-none font-bold nums sm:text-5xl">
                        {date.day}
                    </span>
                    <span className="mt-1 text-sm font-medium">{date.month}</span>
                    <span className="text-xs text-primary-foreground/70">{date.weekday}</span>
                </div>
                <div className="min-w-0 flex-1 border-l-2 border-dashed border-border p-6 sm:p-8">
                    <h1 className="text-3xl leading-[1.05] font-bold tracking-[-0.02em] [font-stretch:85%] sm:text-5xl">
                        {title}
                    </h1>
                    <p className="mt-4 text-base text-muted-foreground sm:text-lg">
                        {formatEventDay(startsAt)},{" "}
                        <span className="nums">{formatTimeRange(startsAt, endsAt)}</span>
                    </p>
                    {place && (
                        <p className="mt-1 flex items-center gap-2 text-base text-muted-foreground sm:text-lg">
                            <MapPin className="size-4 shrink-0" aria-hidden />
                            <span className="truncate">{place}</span>
                        </p>
                    )}
                </div>
            </div>
        </div>
    );
}
