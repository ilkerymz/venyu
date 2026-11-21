import { Settings2 } from "lucide-react";
import Link from "next/link";
import type { EventResponse } from "@/api/generated/model";
import { EventStatusBadge } from "@/components/event-status-badge";
import { Button } from "@/components/ui/button";
import { PublishButton } from "./publish-button";

const LINKS = [
    { href: "", label: "Genel" },
    { href: "/katilimcilar", label: "Katılımcılar" },
    { href: "/davetler", label: "Davetler" },
    { href: "/form", label: "Kayıt formu" },
    { href: "/kapi", label: "Kapı" },
];

/**
 * Etkinlik sayfasında sadece organizatörün gördüğü şerit: durum, taslaksa yayınla,
 * yönetim sekmelerine kısayollar. Katılımcının gördüğü sayfa aynı kalır.
 */
export function OrganizerBar({ event }: { event: EventResponse }) {
    const base = `/panel/etkinlik/${event.id}`;
    return (
        <section aria-label="Etkinlik yönetimi" className="mx-auto max-w-6xl px-5 sm:px-8">
            <div className="flex flex-wrap items-center gap-x-6 gap-y-3 rounded-xl bg-foreground px-5 py-4 text-background">
                <div className="flex items-center gap-3">
                    <Settings2 className="size-5 shrink-0 opacity-70" aria-hidden />
                    <span className="font-medium">Bu etkinlik senin</span>
                    <EventStatusBadge status={event.status} />
                </div>
                <nav aria-label="Yönetim" className="flex flex-wrap gap-1">
                    {LINKS.map((link) => (
                        <Button
                            key={link.label}
                            variant="ghost"
                            size="sm"
                            className="text-background hover:bg-background/10 hover:text-background"
                            asChild
                        >
                            <Link href={`${base}${link.href}`}>{link.label}</Link>
                        </Button>
                    ))}
                </nav>
                {event.status === "DRAFT" && (
                    <div className="sm:ml-auto">
                        <PublishButton eventId={event.id ?? ""} />
                    </div>
                )}
            </div>
        </section>
    );
}
