import { SiteHeader } from "@/components/site-header";

function Block({ className }: { className: string }) {
    return <div className={`rounded-md bg-muted motion-safe:animate-pulse ${className}`} />;
}

export default function Loading() {
    return (
        <>
            <SiteHeader />
            <main
                aria-busy
                aria-label="Etkinlik yükleniyor"
                className="mx-auto grid max-w-6xl gap-10 px-5 pt-4 pb-20 sm:px-8 lg:grid-cols-[1fr_22rem] lg:gap-12 lg:pt-8"
            >
                <div>
                    <Block className="h-44 rounded-xl" />
                    <Block className="mt-12 h-5 w-40" />
                    <Block className="mt-4 h-24" />
                </div>
                <Block className="h-72 rounded-xl" />
            </main>
        </>
    );
}
