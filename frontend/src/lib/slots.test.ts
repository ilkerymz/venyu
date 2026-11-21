import { describe, expect, it } from "vitest";
import { activeBookingBySlot, groupByDay } from "./slots";

describe("groupByDay", () => {
    it("İstanbul gününe göre gruplar; gece yarısından sonra başlayan slot ertesi güne düşer", () => {
        const groups = groupByDay([
            { id: "b", startsAt: "2026-12-02T18:00:00Z" },
            { id: "a", startsAt: "2026-12-02T07:00:00Z" },
            { id: "c", startsAt: "2026-12-02T21:30:00Z" }, // 00:30 İstanbul, 3 Aralık
        ]);
        expect(groups.map(([day, items]) => [day, items.map((i) => i.id)])).toEqual([
            ["2026-12-02", ["a", "b"]],
            ["2026-12-03", ["c"]],
        ]);
    });
});

describe("activeBookingBySlot", () => {
    it("sadece bekleyen ve onaylı rezervasyonları slotla eşler", () => {
        const map = activeBookingBySlot([
            { id: "1", slotId: "s1", status: "REJECTED" },
            { id: "2", slotId: "s1", status: "HELD" },
            { id: "3", slotId: "s2", status: "EXPIRED" },
        ]);
        expect(map.get("s1")?.id).toBe("2");
        expect(map.has("s2")).toBe(false);
    });
});
