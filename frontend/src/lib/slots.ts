import type { BookingResponse, SlotResponse } from "@/api/generated/model";

const dayKey = new Intl.DateTimeFormat("en-CA", { timeZone: "Europe/Istanbul" });

/** Slotları İstanbul gününe göre gruplar (sıralı): [["2026-12-02", [...]], ...]. */
export function groupByDay<T extends { startsAt?: string }>(items: T[]): [string, T[]][] {
    const groups = new Map<string, T[]>();
    for (const item of [...items].sort((a, b) =>
        (a.startsAt ?? "").localeCompare(b.startsAt ?? ""),
    )) {
        const key = dayKey.format(new Date(item.startsAt ?? 0));
        groups.set(key, [...(groups.get(key) ?? []), item]);
    }
    return [...groups.entries()];
}

const ACTIVE = new Set(["HELD", "CONFIRMED"]);

/** Slot başına en fazla bir aktif rezervasyon var (ADR-006, partial unique index). */
export function activeBookingBySlot(bookings: BookingResponse[]): Map<string, BookingResponse> {
    return new Map(
        bookings.filter((b) => ACTIVE.has(b.status ?? "")).map((b) => [b.slotId ?? "", b]),
    );
}

export const slotById = (slots: SlotResponse[]) => new Map(slots.map((s) => [s.id ?? "", s]));
