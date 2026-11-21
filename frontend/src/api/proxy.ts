/**
 * BFF proxy'sinin saf yardımcıları (ADR-011). Route handler: `app/api/[...path]/route.ts`.
 * Header'lar allowlist ile geçer: tarayıcının cookie'leri backend'e, backend'in
 * Set-Cookie'si tarayıcıya sızmaz. Authorization header'ını route handler cookie'den kendisi koyar.
 */
const REQUEST_HEADERS = ["accept", "accept-language", "content-type"];
const RESPONSE_HEADERS = ["content-type", "cache-control", "location"];

export function upstreamUrl(base: string, segments: string[], search: string): string {
    const path = segments.map(encodeURIComponent).join("/");
    return `${base.replace(/\/+$/, "")}/${path}${search}`;
}

function pick(source: Headers, names: string[]): Headers {
    const result = new Headers();
    for (const name of names) {
        const value = source.get(name);
        if (value !== null) result.set(name, value);
    }
    return result;
}

export const pickRequestHeaders = (headers: Headers) => pick(headers, REQUEST_HEADERS);
export const pickResponseHeaders = (headers: Headers) => pick(headers, RESPONSE_HEADERS);
