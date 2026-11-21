import { describe, expect, it } from "vitest";
import { eventRange } from "./datetime";

describe("eventRange", () => {
    it("aynı gün içindeki saatleri İstanbul saatiyle yazar", () => {
        expect(eventRange("2026-12-02", "19:00", "21:30")).toEqual({
            startsAt: "2026-12-02T19:00:00+03:00",
            endsAt: "2026-12-02T21:30:00+03:00",
            overnight: false,
        });
    });

    it("bitiş başlangıçtan önceyse ertesi güne taşır (ay ve yıl sonu dahil)", () => {
        expect(eventRange("2026-12-31", "22:00", "02:00").endsAt).toBe("2027-01-01T02:00:00+03:00");
        expect(eventRange("2026-11-30", "23:00", "01:00").endsAt).toBe("2026-12-01T01:00:00+03:00");
        expect(eventRange("2026-12-02", "20:00", "20:00").overnight).toBe(true);
    });
});
