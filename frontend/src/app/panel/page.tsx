import { CalendarPlus } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { EventStatusBadge } from "@/components/event-status-badge";
import { SiteHeader } from "@/components/site-header";
import { Button } from "@/components/ui/button";
import { eventDateParts, formatEventDay, formatTimeRange } from "@/lib/format";
import { loadMyBookings, loadMyEvents } from "./data";
import { MyBookings } from "./my-bookings";

export const metadata: Metadata = { title: "Etkinliklerim", robots: { index: false } };

export default async function PanelPage() {
    const [allEvents, bookings] = await Promise.all([loadMyEvents(), loadMyBookings()]);
    const events = allEvents.toSorted((a, b) => (b.startsAt ?? "").localeCompare(a.startsAt ?? ""));

    return (
        <>
            <SiteHeader />
            <main className="mx-auto max-w-4xl px-5 pt-6 pb-20 sm:px-8">
                <div className="flex flex-wrap items-end justify-between gap-4">
                    <h1 className="text-4xl font-bold tracking-[-0.02em] [font-stretch:85%] sm:text-5xl">
                        Etkinliklerim
                    </h1>
                    <div className="flex gap-2">
                        <Button variant="outline" asChild>
                            <Link href="/mekanlar">Mekan bul</Link>
                        </Button>
                        {events.length > 0 && (
                            <Button asChild>
                                <Link href="/panel/etkinlik/yeni">Yeni etkinlik</Link>
                            </Button>
                        )}
                    </div>
                </div>

                {events.length === 0 ? (
                    <section className="mt-10 rounded-xl border bg-card px-6 py-12 text-center">
                        <CalendarPlus className="mx-auto size-10 text-primary" aria-hidden />
                        <h2 className="mt-4 text-2xl font-semibold">İlk etkinliğini kur</h2>
                        <p className="mx-auto mt-2 max-w-md leading-relaxed text-muted-foreground">
                            Başlığı, zamanı ve yeri gir; biletleri tanımla, yayınla. Paylaşacağın
                            bağlantı hazır olur.
                        </p>
                        <Button className="mt-6" size="lg" asChild>
                            <Link href="/panel/etkinlik/yeni">Etkinlik oluştur</Link>
                        </Button>
                    </section>
                ) : (
                    <ul className="mt-10 divide-y rounded-xl border bg-card">
                        {events.map((event) => {
                            const date = eventDateParts(event.startsAt ?? "");
                            return (
                                <li key={event.id}>
                                    <Link
                                        href={`/panel/etkinlik/${event.id}`}
                                        className="flex items-center gap-4 p-4 outline-none hover:bg-muted/50 focus-visible:ring-[3px] focus-visible:ring-ring/50 sm:gap-5 sm:p-5"
                                    >
                                        <div className="flex size-14 shrink-0 flex-col items-center justify-center rounded-lg bg-primary text-primary-foreground">
                                            <span className="font-display text-xl leading-none font-bold nums">
                                                {date.day}
                                            </span>
                                            <span className="text-xs">{date.month}</span>
                                        </div>
                                        <div className="min-w-0 flex-1">
                                            <p className="truncate font-semibold">{event.title}</p>
                                            <p className="mt-1 text-sm text-muted-foreground nums">
                                                {formatEventDay(event.startsAt ?? "")},{" "}
                                                {formatTimeRange(
                                                    event.startsAt ?? "",
                                                    event.endsAt ?? "",
                                                )}
                                            </p>
                                        </div>
                                        <EventStatusBadge status={event.status} />
                                    </Link>
                                </li>
                            );
                        })}
                    </ul>
                )}
                {bookings.length > 0 && (
                    <section className="mt-12" aria-labelledby="bookings-heading">
                        <h2 id="bookings-heading" className="mb-4 text-2xl font-semibold">
                            Mekan rezervasyonların
                        </h2>
                        <MyBookings bookings={bookings} />
                    </section>
                )}
            </main>
        </>
    );
}
