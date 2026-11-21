import Link from "next/link";
import { Suspense } from "react";
import { hasSession } from "@/auth/server";
import { Button } from "@/components/ui/button";
import { Wordmark } from "@/components/wordmark";

/** Oturum durumu cookie'den okunur (istek başına dinamik); sayfanın geri kalanı statik kalabilir. */
async function AccountLinks() {
    if (await hasSession()) {
        return (
            <Button variant="ghost" asChild>
                <Link href="/hesabim">Hesabım</Link>
            </Button>
        );
    }
    return (
        <>
            <Button variant="ghost" asChild>
                <Link href="/giris">Giriş yap</Link>
            </Button>
            <Button asChild>
                <Link href="/kayit">Kayıt ol</Link>
            </Button>
        </>
    );
}

export function SiteHeader({ children }: { children?: React.ReactNode }) {
    return (
        <header className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-5 py-5 sm:px-8">
            <Wordmark />
            <nav className="flex items-center gap-1 sm:gap-2" aria-label="Hesap">
                {children}
                <Suspense fallback={<span className="inline-block h-9 w-24" />}>
                    <AccountLinks />
                </Suspense>
            </nav>
        </header>
    );
}
