import { Settings2 } from "lucide-react";
import type { EventResponse } from "@/api/generated/model";
import { EventStatusBadge } from "@/components/event-status-badge";
import { OrganizerNav } from "@/components/organizer-nav";
import { PublishButton } from "@/components/publish-button";
import { cn } from "@/lib/utils";

/**
 * Organizatörün etkinlik şeridi: etkinlik sayfasında ve yönetim sekmelerinde aynı yerde, aynı biçimde.
 * Durum, sekmeler (bulunulan sekme vurgulu) ve `publish` ise taslakta "Etkinliği yayınla"
 * (yönetimde bu düğme Genel sekmesinin Durum kartında; iki kez görünmesin).
 */
export function OrganizerBar({
    event,
    publish = false,
    className,
}: {
    event: EventResponse;
    publish?: boolean;
    className?: string;
}) {
    return (
        <section
            aria-label="Etkinlik yönetimi"
            className={cn("mx-auto w-full max-w-6xl px-5 sm:px-8", className)}
        >
            <div className="flex flex-wrap items-center gap-x-6 gap-y-3 rounded-xl bg-foreground px-4 py-3 text-background sm:px-5">
                <div className="flex items-center gap-3">
                    <Settings2 className="size-5 shrink-0 opacity-70" aria-hidden />
                    <span className="font-medium whitespace-nowrap">Bu etkinlik senin</span>
                    <EventStatusBadge status={event.status} />
                </div>
                <OrganizerNav eventId={event.id ?? ""} slug={event.slug ?? ""} />
                {publish && event.status === "DRAFT" && (
                    <div className="sm:ml-auto">
                        <PublishButton eventId={event.id ?? ""} />
                    </div>
                )}
            </div>
        </section>
    );
}
