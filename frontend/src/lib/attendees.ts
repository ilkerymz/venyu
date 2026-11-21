import type { AttendeeResponse, FormField } from "@/api/generated/model";

export const ATTENDEE_STATUS: Record<string, string> = {
    CONFIRMED: "Kayıtlı",
    CHECKED_IN: "Girdi",
    RESERVED: "Onay bekliyor",
    CANCELLED: "İptal etti",
    PENDING_THRESHOLD: "Eşik bekliyor",
    REFUNDED: "İade edildi",
};

export function formatAnswer(value: unknown): string {
    if (value === true) return "Evet";
    if (value === false) return "Hayır";
    if (Array.isArray(value)) return value.join(", ");
    return value === null || value === undefined ? "" : String(value);
}

/**
 * Cevapları en güncel formun soru metinleriyle eşler; formdan sonradan silinmiş bir sorunun
 * cevabı "Silinmiş soru" olarak gösterilir (soru metni eski form sürümünde kaldı).
 */
export function labeledAnswers(
    attendee: AttendeeResponse,
    fields: FormField[],
): { label: string; value: string }[] {
    const labels = new Map(fields.map((f) => [f.key, f.label]));
    return Object.entries(attendee.answers ?? {}).map(([key, value]) => ({
        label: labels.get(key) ?? "Silinmiş soru",
        value: formatAnswer(value),
    }));
}

const cell = (value: string) => `"${value.replace(/"/g, '""')}"`;

/**
 * Excel'in Türkçe ayarlarında doğrudan açılsın diye: UTF-8 BOM + `;` ayırıcı.
 * Formül enjeksiyonuna karşı `=`, `+`, `-`, `@` ile başlayan değerlerin başına `'` eklenir.
 */
export function attendeesCsv(attendees: AttendeeResponse[], fields: FormField[]): string {
    const safe = (value: string) => cell(/^[=+\-@]/.test(value) ? `'${value}` : value);
    const header = ["Ad", "E-posta", "Tür", "Durum", "Giriş", ...fields.map((f) => f.label)];
    const rows = attendees.map((a) => [
        a.name ?? "",
        a.email ?? "",
        a.guest ? "Misafir" : "Üye",
        ATTENDEE_STATUS[a.status ?? ""] ?? a.status ?? "",
        a.checkedInAt ?? "",
        ...fields.map((f) => formatAnswer(a.answers?.[f.key])),
    ]);
    return "﻿" + [header, ...rows].map((row) => row.map(safe).join(";")).join("\r\n");
}
