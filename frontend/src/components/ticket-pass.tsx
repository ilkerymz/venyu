import { Badge } from "@/components/ui/badge";
import { TicketQr } from "@/components/ticket-qr";

const STATUS: Record<string, { label: string; className: string }> = {
    CONFIRMED: { label: "Onaylandı", className: "bg-success-soft text-success" },
    CHECKED_IN: { label: "Giriş yapıldı", className: "bg-secondary text-secondary-foreground" },
    RESERVED: { label: "Onay bekliyor", className: "bg-muted text-muted-foreground" },
    CANCELLED: { label: "İptal edildi", className: "bg-scarce-soft text-scarce" },
    EXPIRED: { label: "Süresi doldu", className: "bg-scarce-soft text-scarce" },
};

/** Kapıda gösterilecek bilet: QR + kod. Kod okunamazsa görevli elle girebilsin diye yazıyla da var. */
export function TicketPass({
    code,
    status,
    title,
}: {
    code?: string;
    status?: string;
    title?: string;
}) {
    const s = status ? STATUS[status] : undefined;
    return (
        <div className="grid justify-items-center gap-4 text-center">
            {title && <p className="font-display text-lg font-semibold">{title}</p>}
            {code && status !== "CANCELLED" && status !== "EXPIRED" ? (
                <TicketQr code={code} className="size-52 rounded-lg border bg-white p-1" />
            ) : null}
            {code && status !== "CANCELLED" && (
                <code className="text-xs break-all text-muted-foreground">{code}</code>
            )}
            {s && <Badge className={s.className}>{s.label}</Badge>}
        </div>
    );
}
