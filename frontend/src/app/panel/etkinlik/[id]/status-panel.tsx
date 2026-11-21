"use client";

import { Check, Copy, ExternalLink } from "lucide-react";
import { useActionState, useState, useSyncExternalStore } from "react";
import { FormErrors, SubmitButton } from "@/components/form-parts";
import { Button } from "@/components/ui/button";
import { publishEventAction } from "../../actions";

type Props = {
    eventId: string;
    status?: string;
    slug?: string;
    visibility?: string;
    cancellationReason?: string;
};

export function StatusPanel({ eventId, status, slug, visibility, cancellationReason }: Props) {
    if (status === "DRAFT") return <PublishForm eventId={eventId} />;
    if (status === "CANCELLED") {
        return (
            <p className="leading-relaxed">
                Etkinlik iptal edildi; kayıtlı katılımcılara bildirim gönderildi.
                {cancellationReason && (
                    <span className="mt-2 block text-muted-foreground">
                        Neden: {cancellationReason}
                    </span>
                )}
            </p>
        );
    }
    return <ShareLink slug={slug ?? ""} inviteOnly={visibility === "INVITE_ONLY"} />;
}

function PublishForm({ eventId }: { eventId: string }) {
    const [state, action] = useActionState(publishEventAction.bind(null, eventId), {});
    return (
        <form action={action} className="grid gap-4">
            <p className="leading-relaxed text-muted-foreground">
                Etkinlik taslak; sayfası henüz kimseye açık değil. Biletleri kontrol et, hazır
                olunca yayınla.
            </p>
            <FormErrors errors={state.errors} />
            <SubmitButton pendingText="Yayınlanıyor…" className="justify-self-start">
                Etkinliği yayınla
            </SubmitButton>
        </form>
    );
}

function ShareLink({ slug, inviteOnly }: { slug: string; inviteOnly: boolean }) {
    const path = `/e/${slug}`;
    // Sunucuda origin bilinmez; tarayıcıda tam adres gösterilir.
    const origin = useSyncExternalStore(
        () => () => {},
        () => window.location.origin,
        () => "",
    );
    const [copied, setCopied] = useState(false);
    const url = `${origin}${path}`;

    if (inviteOnly) {
        return (
            <p className="leading-relaxed text-muted-foreground">
                Yayında. Etkinlik sadece davetlilere açık; davet gönderme ekranı yakında burada.
            </p>
        );
    }

    async function copy() {
        await navigator.clipboard.writeText(url);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    }

    return (
        <div className="grid gap-3">
            <p className="text-muted-foreground">Yayında. Etkinlik sayfasının bağlantısı:</p>
            <div className="flex flex-wrap items-center gap-2">
                <code className="min-w-0 flex-1 truncate rounded-md bg-muted px-3 py-2 text-sm">
                    {url}
                </code>
                <Button type="button" variant="outline" onClick={copy}>
                    {copied ? <Check aria-hidden /> : <Copy aria-hidden />}
                    {copied ? "Kopyalandı" : "Kopyala"}
                </Button>
                <Button variant="ghost" asChild>
                    <a href={path} target="_blank" rel="noreferrer">
                        <ExternalLink aria-hidden />
                        Sayfayı aç
                    </a>
                </Button>
            </div>
        </div>
    );
}
