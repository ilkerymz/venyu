"use client";

import { Button } from "@/components/ui/button";
import { HeaderBar } from "@/components/header-bar";

/** Backend'e ulaşılamadığında veya beklenmeyen bir hata olduğunda (404 hariç; o not-found.tsx). */
export default function EventError({ retry }: { error: unknown; retry: () => void }) {
    return (
        <>
            <HeaderBar />
            <main className="mx-auto max-w-xl px-5 py-24 sm:px-8">
                <h1 className="text-4xl font-bold tracking-tight">Etkinlik şu an yüklenemedi</h1>
                <p className="mt-4 text-lg leading-relaxed text-muted-foreground">
                    Sunucuya ulaşırken bir sorun oldu. Birkaç saniye sonra yeniden dene.
                </p>
                <Button className="mt-8" onClick={() => retry()}>
                    Yeniden dene
                </Button>
            </main>
        </>
    );
}
