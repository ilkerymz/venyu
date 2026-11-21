import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { SiteHeader } from "@/components/site-header";
import { loadMyBookings } from "../../data";
import { NewEventForm } from "./new-event-form";

export const metadata: Metadata = { title: "Yeni etkinlik", robots: { index: false } };

type Props = { searchParams: Promise<{ rezervasyon?: string }> };

export default function NewEventPage({ searchParams }: Props) {
    return (
        <>
            <SiteHeader />
            <main className="mx-auto max-w-2xl px-5 pt-6 pb-20 sm:px-8">
                <Link
                    href="/hesabim"
                    className="text-sm text-muted-foreground underline-offset-4 hover:underline"
                >
                    Etkinliklerim
                </Link>
                <h1 className="mt-2 mb-8 text-4xl font-bold tracking-[-0.02em] [font-stretch:85%] sm:text-5xl">
                    Yeni etkinlik
                </h1>
                <Suspense
                    fallback={
                        <div className="h-96 rounded-xl bg-muted motion-safe:animate-pulse" />
                    }
                >
                    <FormWithBookings searchParams={searchParams} />
                </Suspense>
            </main>
        </>
    );
}

/** Onaylı ve henüz bir etkinliğe bağlanmamış rezervasyonlar "Venyu mekanında" seçeneği olur. */
async function FormWithBookings({ searchParams }: Props) {
    const [{ rezervasyon }, bookings] = await Promise.all([searchParams, loadMyBookings()]);
    const usable = bookings.filter((b) => b.booking.status === "CONFIRMED" && !b.event);
    return <NewEventForm bookings={usable} preselected={rezervasyon} />;
}
