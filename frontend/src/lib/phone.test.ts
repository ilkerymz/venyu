import { describe, expect, it } from "vitest";
import { formatPhone, normalizePhone } from "./phone";

describe("normalizePhone", () => {
    it("Türkiye numarasının yazılış biçimlerini E.164'e çevirir", () => {
        for (const input of [
            "0532 123 45 67",
            "532-123-4567",
            "(0532) 123 45 67",
            "+90 532 123 45 67",
            "0090 532 123 4567",
        ]) {
            expect(normalizePhone(input)).toBe("+905321234567");
        }
    });

    it("eksik ya da hatalı numarayı reddeder", () => {
        expect(normalizePhone("0532 123")).toBeUndefined();
        expect(normalizePhone("+0 123")).toBeUndefined();
        expect(normalizePhone("abc")).toBeUndefined();
    });

    it("yabancı numarayı olduğu gibi kabul eder", () => {
        expect(normalizePhone("+49 151 23456789")).toBe("+4915123456789");
    });
});

describe("formatPhone", () => {
    it("Türkiye numarasını okunur yazar", () => {
        expect(formatPhone("+905321234567")).toBe("+90 532 123 45 67");
        expect(formatPhone("+4915123456789")).toBe("+4915123456789");
    });
});
