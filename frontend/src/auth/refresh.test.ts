import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { refreshSession } from "./refresh";

const tokens = { accessToken: "a2", refreshToken: "r2", expiresIn: 900 };

describe("refreshSession", () => {
    const fetchMock = vi.fn();

    beforeEach(() => {
        fetchMock.mockReset();
        vi.stubGlobal("fetch", fetchMock);
    });
    afterEach(() => vi.unstubAllGlobals());

    it("aynı refresh token'la eşzamanlı istekler backend'i bir kez çağırır", async () => {
        fetchMock.mockImplementation(async () => Response.json(tokens));
        const results = await Promise.all(
            Array.from({ length: 5 }, () => refreshSession("r1-concurrent", "http://api")),
        );
        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(results.every((r) => r.kind === "ok" && r.tokens.accessToken === "a2")).toBe(true);
    });

    it("yenilemeden kısa süre sonra gelen eski token'lı istek aynı sonucu alır", async () => {
        fetchMock.mockImplementation(async () => Response.json(tokens));
        let time = 1_000;
        const now = () => time;
        await refreshSession("r1-grace", "http://api", now);
        time += 5_000;
        const second = await refreshSession("r1-grace", "http://api", now);
        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(second.kind).toBe("ok");

        time += 60_000; // grace bitti: tekrar backend'e gider
        await refreshSession("r1-grace", "http://api", now);
        expect(fetchMock).toHaveBeenCalledTimes(2);
    });

    it("401'i geçersiz, ağ hatasını ve 5xx'i ulaşılamaz sayar", async () => {
        fetchMock.mockResolvedValueOnce(new Response(null, { status: 401 }));
        expect((await refreshSession("r-401", "http://api")).kind).toBe("invalid");

        fetchMock.mockRejectedValueOnce(new TypeError("fetch failed"));
        expect((await refreshSession("r-down", "http://api")).kind).toBe("unavailable");

        fetchMock.mockResolvedValueOnce(new Response(null, { status: 503 }));
        expect((await refreshSession("r-503", "http://api")).kind).toBe("unavailable");
    });

    it("ulaşılamaz sonucu önbelleğe almaz; sonraki istek tekrar dener", async () => {
        fetchMock.mockRejectedValueOnce(new TypeError("fetch failed"));
        await refreshSession("r-retry", "http://api");
        fetchMock.mockResolvedValueOnce(Response.json(tokens));
        expect((await refreshSession("r-retry", "http://api")).kind).toBe("ok");
    });
});
