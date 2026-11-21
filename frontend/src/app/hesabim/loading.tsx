import { SiteHeader } from "@/components/site-header";

export default function Loading() {
    return (
        <>
            <SiteHeader />
            <main
                aria-busy
                aria-label="Hesap yükleniyor"
                className="mx-auto grid max-w-6xl gap-10 px-5 pt-6 sm:px-8 lg:grid-cols-[20rem_1fr] lg:gap-12"
            >
                <div className="h-80 rounded-xl bg-muted motion-safe:animate-pulse" />
                <div>
                    <div className="h-10 w-56 rounded-md bg-muted motion-safe:animate-pulse" />
                    <div className="mt-6 grid gap-5 sm:grid-cols-2">
                        <div className="h-36 rounded-xl bg-muted motion-safe:animate-pulse" />
                        <div className="h-36 rounded-xl bg-muted motion-safe:animate-pulse" />
                    </div>
                </div>
            </main>
        </>
    );
}
