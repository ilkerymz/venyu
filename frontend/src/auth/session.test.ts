import { describe, expect, it } from "vitest";
import { accessMaxAge, isProtectedPath, safeNextPath } from "./session";

describe("safeNextPath", () => {
    it("site içi yolları kabul eder", () => {
        expect(safeNextPath("/hesabim")).toBe("/hesabim");
        expect(safeNextPath("/e/abc?x=1")).toBe("/e/abc?x=1");
    });

    it("dış adreslere yönlendirmez", () => {
        for (const next of ["https://kotu.com", "//kotu.com", "/\\kotu.com", "", undefined, 42]) {
            expect(safeNextPath(next)).toBe("/hesabim");
        }
    });
});

describe("oturum yardımcıları", () => {
    it("access cookie'si token'dan 30 sn önce düşer", () => {
        expect(accessMaxAge(900)).toBe(870);
        expect(accessMaxAge(10)).toBe(1);
    });

    it("korunan yolları tanır", () => {
        expect(isProtectedPath("/hesabim")).toBe(true);
        expect(isProtectedPath("/panel/etkinlik/1")).toBe(true);
        expect(isProtectedPath("/hesabimiz")).toBe(false);
        expect(isProtectedPath("/e/abc")).toBe(false);
    });
});
