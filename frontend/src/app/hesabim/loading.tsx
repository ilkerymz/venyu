import { Wordmark } from "@/components/wordmark";

export default function Loading() {
    return (
        <>
            <header className="mx-auto max-w-6xl px-5 py-5 sm:px-8">
                <Wordmark />
            </header>
            <main
                aria-busy
                aria-label="Hesap yükleniyor"
                className="mx-auto max-w-3xl px-5 pt-6 sm:px-8"
            >
                <div className="h-12 w-64 rounded-md bg-muted motion-safe:animate-pulse" />
                <div className="mt-10 h-32 rounded-xl bg-muted motion-safe:animate-pulse" />
                <div className="mt-6 h-40 rounded-xl bg-muted motion-safe:animate-pulse" />
            </main>
        </>
    );
}
