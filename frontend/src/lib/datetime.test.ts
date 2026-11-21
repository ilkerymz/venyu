import { describe, expect, it } from "vitest";
import { eventRange, splitByTime } from "./datetime";

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

describe("splitByTime", () => {
    const now = Date.parse("2026-12-05T12:00:00Z");
    const e = (id: string, startsAt: string, endsAt: string) => ({ id, startsAt, endsAt });
    const items = [
        e("gelecek-uzak", "2026-12-20T10:00:00Z", "2026-12-20T12:00:00Z"),
        e("gecmis-eski", "2026-11-01T10:00:00Z", "2026-11-01T12:00:00Z"),
        e("suruyor", "2026-12-05T11:00:00Z", "2026-12-05T13:00:00Z"),
        e("gecmis-yeni", "2026-12-04T10:00:00Z", "2026-12-04T12:00:00Z"),
    ];

    it("süren etkinliği yaklaşanlarda tutar, yaklaşanları en yakından sıralar", () => {
        const { upcoming } = splitByTime(items, (x) => x, now);
        expect(upcoming.map((x) => x.id)).toEqual(["suruyor", "gelecek-uzak"]);
    });

    it("geçmişi en yeniden eskiye sıralar", () => {
        const { past } = splitByTime(items, (x) => x, now);
        expect(past.map((x) => x.id)).toEqual(["gecmis-yeni", "gecmis-eski"]);
    });
});
