import type { NextRequest } from "next/server";
import { pickRequestHeaders, pickResponseHeaders, upstreamUrl } from "@/api/proxy";
import { ACCESS_COOKIE } from "@/auth/session";

/** Tarayıcıdan gelen `/api/*` isteklerini backend'e iletir, oturum cookie'sini Bearer token'a çevirir (ADR-011, BFF). */
const BACKEND_URL = process.env.VENYU_API_URL ?? "http://localhost:8080";

type Context = { params: Promise<{ path: string[] }> };

async function forward(req: NextRequest, { params }: Context): Promise<Response> {
    const { path } = await params;
    const hasBody = req.method !== "GET" && req.method !== "HEAD";
    const headers = pickRequestHeaders(req.headers);
    // Token cookie'den gelir (proxy.ts bu istek için tazeledi); tarayıcı kodu token'ı hiç görmez.
    const accessToken = req.cookies.get(ACCESS_COOKIE)?.value;
    if (accessToken) headers.set("authorization", `Bearer ${accessToken}`);

    let upstream: Response;
    try {
        upstream = await fetch(upstreamUrl(BACKEND_URL, path, req.nextUrl.search), {
            method: req.method,
            headers,
            body: hasBody ? await req.arrayBuffer() : undefined,
            redirect: "manual",
        });
    } catch {
        return Response.json(
            { title: "Sunucuya şu an ulaşılamıyor.", status: 502 },
            { status: 502, headers: { "content-type": "application/problem+json" } },
        );
    }

    return new Response(upstream.body, {
        status: upstream.status,
        headers: pickResponseHeaders(upstream.headers),
    });
}

export { forward as GET, forward as POST, forward as PUT, forward as PATCH, forward as DELETE };
