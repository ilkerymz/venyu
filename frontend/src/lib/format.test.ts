import { describe, expect, it } from "vitest";
import { eventDateParts, formatEventDay, formatPrice, formatTimeRange } from "./format";

describe("formatPrice", () => {
    it("sıfır tutarı ücretsiz olarak gösterir", () => {
        expect(formatPrice(0, "TRY")).toBe("Ücretsiz");
    });

    it("tam sayıda kuruş göstermez, kesirli tutarda iki hane gösterir", () => {
        expect(formatPrice(250, "TRY")).toBe("₺250");
        expect(formatPrice(1250.5, "TRY")).toBe("₺1.250,50");
    });
});

describe("tarih biçimleri", () => {
    // 16:30 UTC = 19:30 İstanbul (UTC+3, yaz saati yok)
    const start = "2026-11-25T16:30:00Z";
    const end = "2026-11-25T19:00:00Z";

    it("UTC zamanı İstanbul saatine çevirir", () => {
        expect(formatTimeRange(start, end)).toBe("19:30–22:00");
    });

    it("gün adını ve ayı Türkçe yazar", () => {
        expect(formatEventDay(start)).toBe("25 Kasım Çarşamba");
        expect(eventDateParts(start)).toEqual({ day: "25", month: "Kas", weekday: "Çar" });
    });

    it("gece yarısını geçen UTC zamanında İstanbul gününü kullanır", () => {
        // 22:30 UTC = ertesi gün 01:30 İstanbul
        expect(eventDateParts("2026-11-25T22:30:00Z").day).toBe("26");
    });
});
