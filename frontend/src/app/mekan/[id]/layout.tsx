import { MapPin, Users } from "lucide-react";
import Link from "next/link";
import { Suspense } from "react";
import { SectionTabs } from "@/components/section-tabs";
import { SiteHeader } from "@/components/site-header";
import { loadOwnedVenue } from "../data";

const TABS = [
    { href: "", label: "Takvim" },
    { href: "/rezervasyonlar", label: "Rezervasyonlar" },
    { href: "/bilgiler", label: "Bilgiler" },
];

type Props = { children: React.ReactNode; params: Promise<{ id: string }> };

export default function VenueLayout({ children, params }: Props) {
    return (
        <>
            <SiteHeader />
            <main className="mx-auto max-w-5xl px-5 pt-6 pb-20 sm:px-8">
                <Link
                    href="/mekan"
                    className="text-sm text-muted-foreground underline-offset-4 hover:underline"
                >
                    Mekanlarım
                </Link>
                <Suspense
                    fallback={
                        <div className="mt-2 h-12 w-72 max-w-full rounded-md bg-muted motion-safe:animate-pulse" />
                    }
                >
                    <VenueHeader params={params} />
                </Suspense>
                <SectionTabs prefix="/mekan" tabs={TABS} label="Mekan yönetimi" />
                <div className="mt-8">{children}</div>
            </main>
        </>
    );
}

async function VenueHeader({ params }: { params: Promise<{ id: string }> }) {
    const venue = await loadOwnedVenue((await params).id);
    return (
        <div className="mt-2">
            <h1 className="text-4xl font-bold tracking-[-0.02em] [font-stretch:85%] sm:text-5xl">
                {venue.name}
            </h1>
            <p className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-muted-foreground">
                <span className="flex items-center gap-1.5">
                    <MapPin className="size-4" aria-hidden />
                    {[venue.district, venue.city].filter(Boolean).join(", ")}
                </span>
                {venue.capacity && (
                    <span className="flex items-center gap-1.5 nums">
                        <Users className="size-4" aria-hidden />
                        {venue.capacity} kişi
                    </span>
                )}
            </p>
        </div>
    );
}
