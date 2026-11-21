import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "./api-error";
import { apiFetch } from "./fetcher";

describe("apiFetch", () => {
    afterEach(() => vi.unstubAllGlobals());

    it("gövdesiz başarılı yanıtta (202/204) data undefined döner", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => new Response(null, { status: 202 })),
        );
        const result = await apiFetch<{ data: unknown; status: number }>(
            "/auth/password-reset/request",
        );
        expect(result).toMatchObject({ data: undefined, status: 202 });
    });

    it("başarısız yanıtta Problem Details taşıyan ApiError fırlatır", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () =>
                Response.json(
                    { status: 409, detail: "Bu e-posta adresiyle kayıtlı bir hesap var." },
                    { status: 409, headers: { "content-type": "application/problem+json" } },
                ),
            ),
        );
        const error = await apiFetch("/auth/register").catch((e: unknown) => e);
        expect(error).toBeInstanceOf(ApiError);
        expect((error as ApiError).status).toBe(409);
    });
});
