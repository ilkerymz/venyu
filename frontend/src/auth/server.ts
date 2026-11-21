import "server-only";
import { cookies } from "next/headers";
import { ACCESS_COOKIE, REFRESH_COOKIE } from "./session";

/**
 * Server Component ve Server Action'larda kullanıcı adına backend çağrısı için header.
 * Üretilen fonksiyonlara açıkça verilir: `me({ headers: await authHeaders() })`.
 * Token `proxy.ts` tarafından bu istek için zaten tazelenmiştir.
 */
export async function authHeaders(): Promise<HeadersInit> {
    const token = (await cookies()).get(ACCESS_COOKIE)?.value;
    return token ? { authorization: `Bearer ${token}` } : {};
}

/** Oturum açık mı? (Ağ çağrısı yapmaz; header'daki "Giriş yap / Hesabım" için yeterli.) */
export async function hasSession(): Promise<boolean> {
    const store = await cookies();
    return store.has(ACCESS_COOKIE) || store.has(REFRESH_COOKIE);
}
