import { Store } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { SiteHeader } from "@/components/site-header";
import { Button } from "@/components/ui/button";
import { loadMyVenues } from "./data";

export const metadata: Metadata = { title: "Mekanlarım", robots: { index: false } };

export default async function MyVenuesPage() {
    const { isOwner, venues } = await loadMyVenues();
    return (
        <>
            <SiteHeader />
            <main className="mx-auto max-w-4xl px-5 pt-6 pb-20 sm:px-8">
                <div className="flex flex-wrap items-end justify-between gap-4">
                    <h1 className="text-4xl font-bold tracking-[-0.02em] [font-stretch:85%] sm:text-5xl">
                        Mekanlarım
                    </h1>
                    {isOwner && venues.length > 0 && (
                        <Button asChild>
                            <Link href="/mekan/yeni">Mekan ekle</Link>
                        </Button>
                    )}
                </div>

                {!isOwner ? (
                    <section className="mt-10 rounded-xl border bg-card p-6 leading-relaxed">
                        <h2 className="text-xl font-semibold">Mekan hesabı gerekiyor</h2>
                        <p className="mt-2 text-muted-foreground">
                            Boş saatlerini Venyu&apos;da listelemek isteyen mekanların hesabını
                            pilot sürecinde biz açıyoruz. Mekanını eklemek için bizimle iletişime
                            geç.
                        </p>
                    </section>
                ) : venues.length === 0 ? (
                    <section className="mt-10 rounded-xl border bg-card px-6 py-12 text-center">
                        <Store className="mx-auto size-10 text-primary" aria-hidden />
                        <h2 className="mt-4 text-2xl font-semibold">İlk mekanını ekle</h2>
                        <p className="mx-auto mt-2 max-w-md leading-relaxed text-muted-foreground">
                            Mekanını tanıt, boş saatlerini takvime ekle; organizatörler o saatleri
                            etkinlikleri için ayırsın.
                        </p>
                        <Button className="mt-6" size="lg" asChild>
                            <Link href="/mekan/yeni">Mekan ekle</Link>
                        </Button>
                    </section>
                ) : (
                    <ul className="mt-10 divide-y rounded-xl border bg-card">
                        {venues.map((venue) => (
                            <li key={venue.id}>
                                <Link
                                    href={`/mekan/${venue.id}`}
                                    className="flex items-center justify-between gap-4 p-5 outline-none hover:bg-muted/50 focus-visible:ring-[3px] focus-visible:ring-ring/50"
                                >
                                    <span className="min-w-0">
                                        <span className="block truncate font-semibold">
                                            {venue.name}
                                        </span>
                                        <span className="block text-sm text-muted-foreground">
                                            {[venue.district, venue.city]
                                                .filter(Boolean)
                                                .join(", ")}
                                        </span>
                                    </span>
                                    {venue.capacity && (
                                        <span className="shrink-0 text-sm text-muted-foreground nums">
                                            {venue.capacity} kişi
                                        </span>
                                    )}
                                </Link>
                            </li>
                        ))}
                    </ul>
                )}
            </main>
        </>
    );
}
