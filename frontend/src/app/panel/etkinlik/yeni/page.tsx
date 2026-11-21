import type { Metadata } from "next";
import Link from "next/link";
import { SiteHeader } from "@/components/site-header";
import { NewEventForm } from "./new-event-form";

export const metadata: Metadata = { title: "Yeni etkinlik", robots: { index: false } };

export default function NewEventPage() {
    return (
        <>
            <SiteHeader />
            <main className="mx-auto max-w-2xl px-5 pt-6 pb-20 sm:px-8">
                <Link
                    href="/panel"
                    className="text-sm text-muted-foreground underline-offset-4 hover:underline"
                >
                    Etkinliklerim
                </Link>
                <h1 className="mt-2 mb-8 text-4xl font-bold tracking-[-0.02em] [font-stretch:85%] sm:text-5xl">
                    Yeni etkinlik
                </h1>
                <NewEventForm />
            </main>
        </>
    );
}
