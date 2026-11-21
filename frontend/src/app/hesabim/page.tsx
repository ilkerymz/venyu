import { CalendarPlus, Plus, Ticket } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { Suspense } from "react";
import { isApiError } from "@/api/api-error";
import { me } from "@/api/generated/me-controller/me-controller";
import { MyBookings } from "@/app/panel/my-bookings";
import { authHeaders } from "@/auth/server";
import { EventCard } from "@/components/event-card";
import { EventStatusBadge } from "@/components/event-status-badge";
import { Notice } from "@/components/form-parts";
import { SiteHeader } from "@/components/site-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import type { TicketState } from "@/lib/registrations";
import { registrationHref } from "@/lib/registrations";
import { BecomeOrganizerForm } from "./account-actions";
import { type EventCardView, loadMyTickets, loadOrganizerArea, type TicketCardView } from "./data";
import { ProfileCard } from "./profile-card";

export const metadata: Metadata = { title: "Hesabım", robots: { index: false } };

type Props = { searchParams: Promise<{ hosgeldin?: string }> };

export default async function AccountPage({ searchParams }: Props) {
    const { hosgeldin } = await searchParams;
    let profile;
    try {
        ({ data: profile } = await me({ headers: await authHeaders() }));
    } catch (error) {
        if (isApiError(error, 401)) redirect("/giris?next=/hesabim");
        throw error;
    }
    const isOrganizer = (profile.roles ?? []).includes("ORGANIZER");

    return (
        <>
            <SiteHeader />
            <main className="mx-auto max-w-6xl px-5 pt-6 pb-20 sm:px-8">
                {hosgeldin && (
                    <div className="mb-8">
                        <Notice>
                            Hesabın hazır. Doğrulama bağlantısını {profile.email} adresine
                            gönderdik.
                        </Notice>
                    </div>
                )}
                <div className="grid gap-10 lg:grid-cols-[20rem_1fr] lg:items-start lg:gap-12">
                    <div className="lg:sticky lg:top-6">
                        <ProfileCard profile={profile} />
                    </div>
                    <div className="min-w-0">
                        {/* Katılımcı için en önemli bilgi biletleri; organizatörde önce kendi etkinlikleri. */}
                        {isOrganizer ? (
                            <>
                                <Suspense fallback={<AreaSkeleton />}>
                                    <OrganizerArea />
                                </Suspense>
                                <Suspense fallback={<AreaSkeleton />}>
                                    <MyTickets className="mt-14" />
                                </Suspense>
                            </>
                        ) : (
                            <>
                                <Suspense fallback={<AreaSkeleton />}>
                                    <MyTickets />
                                </Suspense>
                                <BecomeOrganizer />
                            </>
                        )}
                    </div>
                </div>
            </main>
        </>
    );
}

async function OrganizerArea() {
    const { upcoming, past, bookings } = await loadOrganizerArea();
    const empty = upcoming.length === 0 && past.length === 0;
    return (
        <>
            <section aria-labelledby="events-heading">
                <div className="flex flex-wrap items-end justify-between gap-4">
                    <h2
                        id="events-heading"
                        className="text-3xl font-bold tracking-[-0.02em] [font-stretch:85%]"
                    >
                        Etkinliklerim
                    </h2>
                    <div className="flex gap-2">
                        <Button variant="outline" asChild>
                            <Link href="/mekanlar">Mekan bul</Link>
                        </Button>
                        <Button asChild>
                            <Link href="/panel/etkinlik/yeni">
                                <Plus aria-hidden />
                                Etkinlik oluştur
                            </Link>
                        </Button>
                    </div>
                </div>

                {empty ? (
                    <div className="mt-6 rounded-xl border border-dashed bg-card px-6 py-12 text-center">
                        <CalendarPlus className="mx-auto size-10 text-primary" aria-hidden />
                        <h3 className="mt-4 text-xl font-semibold">İlk etkinliğini kur</h3>
                        <p className="mx-auto mt-2 max-w-md leading-relaxed text-muted-foreground">
                            Bir mekanın boş saatini ayır ya da kendi adresini yaz; biletleri
                            tanımla, yayınla. Paylaşacağın bağlantı hazır olur.
                        </p>
                    </div>
                ) : (
                    <>
                        {upcoming.length > 0 ? (
                            <CardGrid cards={upcoming} />
                        ) : (
                            <p className="mt-6 text-muted-foreground">Yaklaşan etkinliğin yok.</p>
                        )}
                        {past.length > 0 && (
                            <details className="group mt-8">
                                <summary className="cursor-pointer font-medium text-muted-foreground select-none hover:text-foreground">
                                    Geçmiş etkinlikler ({past.length})
                                </summary>
                                <CardGrid cards={past} muted />
                            </details>
                        )}
                    </>
                )}
            </section>

            {bookings.length > 0 && (
                <section id="rezervasyonlar" aria-labelledby="bookings-heading" className="mt-14">
                    <h2 id="bookings-heading" className="mb-4 text-2xl font-semibold">
                        Mekan rezervasyonların
                    </h2>
                    <MyBookings bookings={bookings} />
                </section>
            )}
        </>
    );
}

function CardGrid({ cards, muted }: { cards: EventCardView[]; muted?: boolean }) {
    return (
        <ul className="mt-6 grid gap-5 sm:grid-cols-2">
            {cards.map(({ event, place }) => (
                <li key={event.id}>
                    <EventCard
                        href={`/e/${event.slug}`}
                        title={event.title ?? ""}
                        startsAt={event.startsAt ?? ""}
                        endsAt={event.endsAt ?? ""}
                        place={place}
                        muted={muted || event.status === "CANCELLED"}
                        badge={<EventStatusBadge status={event.status} />}
                    />
                </li>
            ))}
        </ul>
    );
}

const TICKET_BADGE: Record<TicketState, { label: string; className: string }> = {
    ready: { label: "Biletin hazır", className: "bg-success-soft text-success" },
    checkedIn: { label: "Giriş yaptın", className: "bg-secondary text-secondary-foreground" },
    pending: { label: "Onay bekliyor", className: "bg-muted text-muted-foreground" },
    eventCancelled: { label: "Etkinlik iptal edildi", className: "bg-scarce-soft text-scarce" },
};

async function MyTickets({ className }: { className?: string }) {
    const { upcoming, past } = await loadMyTickets();
    return (
        <section aria-labelledby="tickets-heading" className={className}>
            <h2
                id="tickets-heading"
                className="text-3xl font-bold tracking-[-0.02em] [font-stretch:85%]"
            >
                Katıldığım etkinlikler
            </h2>
            {upcoming.length === 0 && past.length === 0 ? (
                <div className="mt-6 flex items-center gap-4 rounded-xl border border-dashed bg-card px-6 py-8">
                    <Ticket className="size-8 shrink-0 text-primary" aria-hidden />
                    <p className="leading-relaxed text-muted-foreground">
                        Henüz bir etkinliğe kayıt olmadın. Kayıt olduğun etkinlikler ve biletlerin
                        burada görünür.
                    </p>
                </div>
            ) : (
                <>
                    {upcoming.length > 0 ? (
                        <TicketGrid cards={upcoming} />
                    ) : (
                        <p className="mt-6 text-muted-foreground">Yaklaşan etkinliğin yok.</p>
                    )}
                    {past.length > 0 && (
                        <details className="mt-8">
                            <summary className="cursor-pointer font-medium text-muted-foreground select-none hover:text-foreground">
                                Geçmiş etkinlikler ({past.length})
                            </summary>
                            <TicketGrid cards={past} muted />
                        </details>
                    )}
                </>
            )}
        </section>
    );
}

function TicketGrid({ cards, muted }: { cards: TicketCardView[]; muted?: boolean }) {
    return (
        <ul className="mt-6 grid gap-5 sm:grid-cols-2">
            {cards.map(({ registration, state, place }) => {
                // Geçmişte "biletin hazır" anlamsız: sadece giriş yaptıysa ya da iptal edildiyse yazılır.
                const badge =
                    muted && (state === "ready" || state === "pending")
                        ? undefined
                        : TICKET_BADGE[state];
                const event = registration.event ?? {};
                return (
                    <li key={registration.id}>
                        <EventCard
                            href={registrationHref(registration)}
                            title={event.title ?? ""}
                            startsAt={event.startsAt ?? ""}
                            endsAt={event.endsAt ?? ""}
                            place={place}
                            muted={muted || state === "eventCancelled"}
                            badge={
                                badge && <Badge className={badge.className}>{badge.label}</Badge>
                            }
                        />
                    </li>
                );
            })}
        </ul>
    );
}

function BecomeOrganizer() {
    return (
        <section
            aria-labelledby="organizer-heading"
            className="mt-14 flex flex-col gap-5 rounded-xl border bg-card p-6 sm:flex-row sm:items-center sm:justify-between"
        >
            <div className="flex gap-4">
                <CalendarPlus className="size-8 shrink-0 text-primary" aria-hidden />
                <div>
                    <h2 id="organizer-heading" className="text-lg font-semibold">
                        Kendi etkinliğini düzenle
                    </h2>
                    <p className="mt-1 max-w-prose text-sm leading-relaxed text-muted-foreground">
                        Organizatör ol; bir mekanın boş saatini ayır ya da kendi adresinde buluşma
                        kur, biletleri ve kayıt formunu tanımla. Ücretsiz.
                    </p>
                </div>
            </div>
            <div className="shrink-0">
                <BecomeOrganizerForm />
            </div>
        </section>
    );
}

function AreaSkeleton() {
    return (
        <div aria-busy aria-label="Etkinlikler yükleniyor">
            <div className="h-10 w-56 rounded-md bg-muted motion-safe:animate-pulse" />
            <div className="mt-6 grid gap-5 sm:grid-cols-2">
                <div className="h-36 rounded-xl bg-muted motion-safe:animate-pulse" />
                <div className="h-36 rounded-xl bg-muted motion-safe:animate-pulse" />
            </div>
        </div>
    );
}
