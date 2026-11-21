import { ApiError, readProblem } from "./api-error";

/**
 * orval'ın ürettiği bütün istekler buradan geçer (ADR-011).
 * - Sunucuda (Server Component) backend'e doğrudan gider: `VENYU_API_URL`.
 * - Tarayıcıda Next.js BFF'ine gider: `/api/*` → backend.
 * Başarısız yanıt `ApiError` fırlatır; üretilen tipler sadece başarılı yanıtı modeller.
 */
export function apiBaseUrl(): string {
    if (typeof window !== "undefined") return "/api";
    return process.env.VENYU_API_URL ?? "http://localhost:8080";
}

export async function apiFetch<T>(url: string, init?: RequestInit): Promise<T> {
    const headers = new Headers(init?.headers);
    headers.set("accept", "application/json");

    const res = await fetch(`${apiBaseUrl()}${url}`, { ...init, headers });
    if (!res.ok) {
        throw new ApiError(res.status, await readProblem(res));
    }

    const data = res.status === 204 ? undefined : await res.json();
    return { data, status: res.status, headers: res.headers } as T;
}
