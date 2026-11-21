import { Button } from "@/components/ui/button";
import { Wordmark } from "@/components/wordmark";
import Link from "next/link";

export default function NotFound() {
    return (
        <>
            <header className="mx-auto max-w-6xl px-5 py-5 sm:px-8">
                <Wordmark />
            </header>
            <main className="mx-auto max-w-xl px-5 py-24 sm:px-8">
                <h1 className="text-4xl font-bold tracking-tight">Bu sayfa bulunamadı</h1>
                <p className="mt-4 text-lg leading-relaxed text-muted-foreground">
                    Adresi kontrol et ya da ana sayfadan devam et.
                </p>
                <Button className="mt-8" variant="outline" asChild>
                    <Link href="/">Ana sayfaya dön</Link>
                </Button>
            </main>
        </>
    );
}
