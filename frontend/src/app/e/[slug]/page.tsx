import { MapPin } from "lucide-react";
import type { Metadata } from "next";
import { unstable_rethrow } from "next/navigation";
import { SiteHeader } from "@/components/site-header";
import { eventDateParts, formatEventDay, formatTimeRange } from "@/lib/format";
import { loadPublicEvent, loadRegistrationContext } from "./data";
import { MyTicket } from "./my-ticket";
import { RegistrationPanel } from "@/components/registration-panel";
import { registerAction } from "./actions";

type Props = { params: Promise<{ slug: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
    let event;
    try {
        ({ event } = await loadPublicEvent((await params).slug));
    } catch (error) {
        // 404 (notFound) Next.js'e kalsın; diğer hataları sayfa gösterir (error.tsx), metadata varsayılana düşer.
        unstable_rethrow(error);
        return {};
    }
    const description = event.description?.slice(0, 160);
    return {
        title: event.title,
        description,
        openGraph: { title: event.title, description, type: "website" },
    };
}

export default async function PublicEventPage({ params }: Props) {
    const { event, venue } = await loadPublicEvent((await params).slug);
    const startsAt = event.startsAt ?? "";
    const endsAt = event.endsAt ?? "";
    const date = eventDateParts(startsAt);
    // Başlıkta kısa yer adı; tam adres "Konum" bölümünde.
    const place = venue
        ? [venue.name, venue.district].filter(Boolean).join(", ")
        : (event.customAddress?.split("\n")[0] ?? "");
    const tickets = event.tickets ?? [];
    const registration = await loadRegistrationContext(event.id ?? "");

    return (
        <>
            <SiteHeader />

            <main className="mx-auto grid max-w-6xl gap-10 px-5 pt-4 pb-20 sm:px-8 lg:grid-cols-[1fr_22rem] lg:gap-12 lg:pt-8">
                <div className="min-w-0">
                    <div className="drop-shadow-[0_18px_30px_rgb(21_23_46/0.12)]">
                        <div className="flex overflow-hidden rounded-xl bg-card ticket-cut">
                            <div className="flex w-20 shrink-0 flex-col items-center justify-center bg-primary py-6 text-primary-foreground sm:w-24">
                                <span className="font-display text-4xl leading-none font-bold nums sm:text-5xl">
                                    {date.day}
                                </span>
                                <span className="mt-1 text-sm font-medium">{date.month}</span>
                                <span className="text-xs text-primary-foreground/70">
                                    {date.weekday}
                                </span>
                            </div>
                            <div className="min-w-0 flex-1 border-l-2 border-dashed border-border p-6 sm:p-8">
                                <h1 className="text-3xl leading-[1.05] font-bold tracking-[-0.02em] [font-stretch:85%] sm:text-5xl">
                                    {event.title}
                                </h1>
                                <p className="mt-4 text-base text-muted-foreground sm:text-lg">
                                    {formatEventDay(startsAt)},{" "}
                                    <span className="nums">
                                        {formatTimeRange(startsAt, endsAt)}
                                    </span>
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

                    {event.description && (
                        <section className="mt-12">
                            <h2 className="text-xl font-semibold">Etkinlik hakkında</h2>
                            <p className="mt-3 max-w-prose text-base leading-relaxed whitespace-pre-line">
                                {event.description}
                            </p>
                        </section>
                    )}

                    <section className="mt-12">
                        <h2 className="text-xl font-semibold">Konum</h2>
                        {venue ? (
                            <address className="mt-3 leading-relaxed not-italic">
                                <span className="font-medium">{venue.name}</span>
                                <br />
                                {venue.address}
                                <br />
                                <span className="text-muted-foreground">
                                    {[venue.district, venue.city].filter(Boolean).join(", ")}
                                </span>
                            </address>
                        ) : (
                            <address className="mt-3 leading-relaxed whitespace-pre-line not-italic">
                                {event.customAddress}
                            </address>
                        )}
                    </section>
                </div>

                <aside className="lg:self-start" aria-labelledby="tickets-heading">
                    <div className="rounded-xl border bg-card p-6">
                        <h2 id="tickets-heading" className="text-xl font-semibold">
                            {registration.myRegistration ? "Biletin" : "Katıl"}
                        </h2>
                        <div className="mt-4">
                            {registration.myRegistration ? (
                                <MyTicket
                                    registrationId={registration.myRegistration.id ?? ""}
                                    ticketCode={registration.myRegistration.ticketCode}
                                    status={registration.myRegistration.status}
                                    ticketName={
                                        tickets.find(
                                            (t) =>
                                                t.id === registration.myRegistration?.ticketTypeId,
                                        )?.name
                                    }
                                />
                            ) : (
                                <RegistrationPanel
                                    action={registerAction.bind(null, event.id ?? "")}
                                    tickets={tickets}
                                    fields={registration.fields}
                                    isMember={registration.isMember}
                                    requiresAccount={event.requiresAccount ?? false}
                                    loginHref={`/giris?next=${encodeURIComponent(`/e/${event.slug}`)}`}
                                />
                            )}
                        </div>
                    </div>
                </aside>
            </main>
        </>
    );
}
