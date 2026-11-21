import { SiteHeader } from "@/components/site-header";

export default function Loading() {
    return (
        <>
            <SiteHeader />
            <main
                aria-busy
                aria-label="Bilet yükleniyor"
                className="mx-auto max-w-xl px-5 pt-6 sm:px-8"
            >
                <div className="h-12 w-72 max-w-full rounded-md bg-muted motion-safe:animate-pulse" />
                <div className="mt-8 h-96 rounded-xl bg-muted motion-safe:animate-pulse" />
            </main>
        </>
    );
}
