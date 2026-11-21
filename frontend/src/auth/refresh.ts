import type { SessionTokens } from "./session";

/**
 * Refresh token'ı yeniler; aynı token için eşzamanlı istekler tek backend çağrısını paylaşır.
 *
 * Neden: backend'de rotation + reuse detection var (ADR-003). İki sekme aynı anda aynı
 * refresh token'la yenilerse ikincisi "yeniden kullanım" sayılır ve bütün oturumlar kapanır.
 * Çözüm: istek uçuştayken aynı promise'i paylaş, bittikten sonra da kısa bir süre (GRACE_MS)
 * aynı sonucu ver; o arada gelen eski token'lı istekler yeni token'ları alır.
 *
 * Sınır: bellek içi; tek Next.js süreci için doğru. Birden fazla instance'a çıkınca
 * paylaşılan bir depo (Redis) veya sticky session gerekir.
 */
export type RefreshResult =
    | { kind: "ok"; tokens: SessionTokens }
    /** Backend token'ı reddetti (süresi dolmuş, iptal edilmiş, yeniden kullanılmış): oturum biter. */
    | { kind: "invalid" }
    /** Backend'e ulaşılamadı: oturum silinmez, sonraki istekte tekrar denenir. */
    | { kind: "unavailable" };

const GRACE_MS = 15_000;

const inFlight = new Map<string, Promise<RefreshResult>>();
const recent = new Map<string, { result: RefreshResult; until: number }>();

export function refreshSession(
    refreshToken: string,
    baseUrl = process.env.VENYU_API_URL ?? "http://localhost:8080",
    now: () => number = Date.now,
): Promise<RefreshResult> {
    const cached = recent.get(refreshToken);
    if (cached && cached.until > now()) return Promise.resolve(cached.result);

    const running = inFlight.get(refreshToken);
    if (running) return running;

    const promise = callBackend(baseUrl, refreshToken).then((result) => {
        inFlight.delete(refreshToken);
        if (result.kind !== "unavailable") {
            recent.set(refreshToken, { result, until: now() + GRACE_MS });
        }
        for (const [key, entry] of recent) {
            if (entry.until <= now()) recent.delete(key);
        }
        return result;
    });
    inFlight.set(refreshToken, promise);
    return promise;
}

async function callBackend(baseUrl: string, refreshToken: string): Promise<RefreshResult> {
    let res: Response;
    try {
        res = await fetch(`${baseUrl}/auth/refresh`, {
            method: "POST",
            headers: { "content-type": "application/json", accept: "application/json" },
            body: JSON.stringify({ refreshToken }),
        });
    } catch {
        return { kind: "unavailable" };
    }
    if (res.ok) return { kind: "ok", tokens: (await res.json()) as SessionTokens };
    if (res.status >= 500) return { kind: "unavailable" };
    return { kind: "invalid" };
}
