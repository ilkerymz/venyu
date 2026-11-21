import { Badge } from "@/components/ui/badge";

type Status = "DRAFT" | "PUBLISHED" | "CONFIRMED" | "COMPLETED" | "SETTLED" | "CANCELLED";

const STATUS: Record<Status, { label: string; className: string }> = {
    DRAFT: { label: "Taslak", className: "bg-muted text-muted-foreground" },
    PUBLISHED: { label: "Yayında", className: "bg-secondary text-secondary-foreground" },
    CONFIRMED: { label: "Kesinleşti", className: "bg-success-soft text-success" },
    COMPLETED: { label: "Tamamlandı", className: "bg-muted text-foreground" },
    SETTLED: { label: "Ödemeler tamamlandı", className: "bg-muted text-foreground" },
    CANCELLED: { label: "İptal edildi", className: "bg-scarce-soft text-scarce" },
};

export function EventStatusBadge({ status }: { status?: string }) {
    const s = STATUS[status as Status] ?? { label: status ?? "", className: "" };
    return <Badge className={s.className}>{s.label}</Badge>;
}

export const VISIBILITY = {
    PUBLIC: {
        label: "Herkese açık",
        hint: "Etkinlik sayfası herkese açık; ileride keşfet akışında da görünecek.",
    },
    UNLISTED: {
        label: "Sadece linki olanlar",
        hint: "Listelerde görünmez; linki paylaştığın kişiler sayfayı açabilir.",
    },
    INVITE_ONLY: {
        label: "Sadece davetliler",
        hint: "Sayfa herkese kapalı; katılımcılar davet bağlantısıyla gelir.",
    },
} as const;
