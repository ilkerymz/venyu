import { type NextRequest, NextResponse } from "next/server";
import { refreshSession } from "@/auth/refresh";
import {
    ACCESS_COOKIE,
    REFRESH_COOKIE,
    clearSession,
    isProtectedPath,
    writeSession,
} from "@/auth/session";

/**
 * Her istekten önce oturumu tazeler (ADR-011):
 * - Access cookie'si düşmüş ama refresh cookie'si varsa token'ları yeniler; yeni token'lar
 *   hem bu isteğe (sayfa, route handler, Server Action aynı istekte görsün) hem tarayıcıya yazılır.
 * - Korunan sayfaya oturumsuz gelen kullanıcıyı girişe yönlendirir.
 * Yetki kontrolünün asıl yeri yine backend'dir; bu katman sadece kullanıcı deneyimi içindir.
 */
export async function proxy(request: NextRequest) {
    const { pathname, search } = request.nextUrl;
    const refreshToken = request.cookies.get(REFRESH_COOKIE)?.value;
    const hasAccess = request.cookies.has(ACCESS_COOKIE);

    if (!hasAccess && refreshToken) {
        const result = await refreshSession(refreshToken);

        if (result.kind === "ok") {
            request.cookies.set(ACCESS_COOKIE, result.tokens.accessToken);
            request.cookies.set(REFRESH_COOKIE, result.tokens.refreshToken);
            const response = NextResponse.next({ request: { headers: request.headers } });
            writeSession(response.cookies, result.tokens);
            return response;
        }

        if (result.kind === "invalid") {
            request.cookies.delete(REFRESH_COOKIE);
            const response = isProtectedPath(pathname)
                ? redirectToLogin(request, pathname + search)
                : NextResponse.next({ request: { headers: request.headers } });
            clearSession(response.cookies);
            return response;
        }
        // unavailable: oturumu silme; sayfa backend hatasını kendisi gösterir.
    }

    if (!refreshToken && isProtectedPath(pathname)) {
        return redirectToLogin(request, pathname + search);
    }
    return NextResponse.next();
}

function redirectToLogin(request: NextRequest, next: string) {
    const url = new URL("/giris", request.url);
    url.searchParams.set("next", next);
    return NextResponse.redirect(url);
}

export const config = {
    // Statik dosyalar ve görseller hariç her istek (sayfalar, /api, Server Action'lar).
    matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
