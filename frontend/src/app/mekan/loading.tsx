import { Wordmark } from "@/components/wordmark";

export default function Loading() {
    return (
        <>
            <header className="mx-auto max-w-6xl px-5 py-5 sm:px-8">
                <Wordmark />
            </header>
            <main aria-busy aria-label="Yükleniyor" className="mx-auto max-w-4xl px-5 pt-6 sm:px-8">
                <div className="h-12 w-72 rounded-md bg-muted motion-safe:animate-pulse" />
                <div className="mt-10 h-64 rounded-xl bg-muted motion-safe:animate-pulse" />
            </main>
        </>
    );
}
