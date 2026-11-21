import "server-only";
import { notFound, redirect } from "next/navigation";
import { isApiError } from "./api-error";

/**
 * Oturum gerektiren sayfa verisi: 401 → giriş (geri dönüş adresiyle), 403 → hesabım
 * (rol yok; hesap sayfası rolü almayı anlatır), 404 → sayfa yok (başkasına ait olanı da gizler).
 */
export async function guard<T>(load: () => Promise<T>, next: string): Promise<T> {
    try {
        return await load();
    } catch (error) {
        if (isApiError(error, 401)) redirect(`/giris?next=${encodeURIComponent(next)}`);
        if (isApiError(error, 403)) redirect("/hesabim");
        if (isApiError(error, 404)) notFound();
        throw error;
    }
}
