import type { Metadata } from "next";
import Link from "next/link";
import { SiteHeader } from "@/components/site-header";
import { createVenueAction } from "../actions";
import { VenueForm } from "../venue-form";

export const metadata: Metadata = { title: "Mekan ekle", robots: { index: false } };

export default function NewVenuePage() {
    return (
        <>
            <SiteHeader />
            <main className="mx-auto max-w-2xl px-5 pt-6 pb-20 sm:px-8">
                <Link
                    href="/mekan"
                    className="text-sm text-muted-foreground underline-offset-4 hover:underline"
                >
                    Mekanlarım
                </Link>
                <h1 className="mt-2 mb-8 text-4xl font-bold tracking-[-0.02em] [font-stretch:85%] sm:text-5xl">
                    Mekan ekle
                </h1>
                <VenueForm action={createVenueAction} submitLabel="Mekanı oluştur" />
            </main>
        </>
    );
}
