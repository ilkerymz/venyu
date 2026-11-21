/**
 * Oturum cookie'leri (ADR-011). İkisi de httpOnly: tarayıcıdaki JavaScript token'ları göremez.
 * Access cookie'si token'dan 30 sn önce düşer; cookie yoksa ama refresh varsa `proxy.ts` yeniler.
 * Bu sayede token'ın süresini çözmek (JWT decode) gerekmez.
 */
export const ACCESS_COOKIE = "venyu_at";
export const REFRESH_COOKIE = "venyu_rt";

const REFRESH_MAX_AGE = 30 * 24 * 60 * 60; // backend: 30 gün (ADR-003)
const EARLY_EXPIRY_SECONDS = 30;

export type SessionTokens = {
    accessToken: string;
    refreshToken: string;
    /** Access token ömrü (saniye). */
    expiresIn: number;
};

/** `cookies()` deposu ile `NextResponse.cookies` ortak arayüzü. */
type CookieWriter = {
    set(name: string, value: string, options: Record<string, unknown>): unknown;
    delete(name: string): unknown;
};

function baseOptions() {
    return {
        httpOnly: true,
        secure: process.env.NODE_ENV === "production",
        sameSite: "lax" as const,
        path: "/",
    };
}

export function accessMaxAge(expiresIn: number): number {
    return Math.max(expiresIn - EARLY_EXPIRY_SECONDS, 1);
}

export function writeSession(cookies: CookieWriter, tokens: SessionTokens): void {
    cookies.set(ACCESS_COOKIE, tokens.accessToken, {
        ...baseOptions(),
        maxAge: accessMaxAge(tokens.expiresIn),
    });
    cookies.set(REFRESH_COOKIE, tokens.refreshToken, {
        ...baseOptions(),
        maxAge: REFRESH_MAX_AGE,
    });
}

export function clearSession(cookies: CookieWriter): void {
    cookies.delete(ACCESS_COOKIE);
    cookies.delete(REFRESH_COOKIE);
}

/** Giriş sonrası yönlendirme: sadece site içi yol (açık yönlendirme açığına karşı). */
export function safeNextPath(next: unknown, fallback = "/hesabim"): string {
    if (typeof next !== "string" || !next.startsWith("/") || next.startsWith("//")) {
        return fallback;
    }
    return next.includes("\\") ? fallback : next;
}

/** Login gerektiren sayfalar; `proxy.ts` oturumsuz isteği girişe yönlendirir. */
export const PROTECTED_PATHS = ["/hesabim", "/panel", "/mekan"];

export function isProtectedPath(pathname: string): boolean {
    return PROTECTED_PATHS.some((p) => pathname === p || pathname.startsWith(`${p}/`));
}
