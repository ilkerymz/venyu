import { describe, expect, it } from "vitest";
import { ApiError, describeError, isApiError, readProblem } from "./api-error";

describe("ApiError", () => {
    it("form hatalarında errors listesini, yoksa detail'i mesaj olarak verir", () => {
        const form = new ApiError(400, {
            title: "Bad Request",
            errors: ["Ad zorunlu.", "Yaş sayı olmalı."],
        });
        expect(form.messages).toEqual(["Ad zorunlu.", "Yaş sayı olmalı."]);

        const conflict = new ApiError(409, {
            title: "Conflict",
            detail: "Bu etkinliğe zaten kayıtlısın.",
        });
        expect(conflict.messages).toEqual(["Bu etkinliğe zaten kayıtlısın."]);
    });

    it("isApiError durum koduna göre ayırt eder", () => {
        const notFound = new ApiError(404, {});
        expect(isApiError(notFound, 404)).toBe(true);
        expect(isApiError(notFound, 409)).toBe(false);
        expect(isApiError(new Error("x"))).toBe(false);
    });
});

describe("readProblem", () => {
    it("JSON olmayan gövdede çökmez", async () => {
        const res = new Response("<html>Bad Gateway</html>", {
            status: 502,
            headers: { "content-type": "text/html" },
        });
        expect(await readProblem(res)).toEqual({ status: 502 });
    });

    it("problem+json gövdesini okur", async () => {
        const res = Response.json(
            { title: "Not Found", status: 404, detail: "Etkinlik bulunamadı." },
            { status: 404, headers: { "content-type": "application/problem+json" } },
        );
        expect((await readProblem(res)).detail).toBe("Etkinlik bulunamadı.");
    });
});

describe("describeError", () => {
    it("backend'in Türkçe mesajını olduğu gibi verir", () => {
        const error = new ApiError(409, { detail: "Bu e-posta adresiyle kayıtlı bir hesap var." });
        expect(describeError(error)).toEqual(["Bu e-posta adresiyle kayıtlı bir hesap var."]);
    });

    it("Spring'in İngilizce doğrulama mesajı yerine Türkçe metin gösterir", () => {
        const error = new ApiError(400, {
            title: "Bad Request",
            detail: "Invalid request content.",
        });
        expect(describeError(error)[0]).toMatch(/Alanları kontrol/);
    });

    it("ağ hatasında sunucuya ulaşılamadığını söyler", () => {
        expect(describeError(new TypeError("fetch failed"))[0]).toMatch(/ulaşılamıyor/);
    });
});
