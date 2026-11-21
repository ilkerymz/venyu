import { CircleUserRound, Plus } from "lucide-react";
import Link from "next/link";
import { Suspense } from "react";
import { hasSession } from "@/auth/server";
import { HeaderBar } from "@/components/header-bar";
import { MainNav, MainNavFallback } from "@/components/main-nav";
import { Button } from "@/components/ui/button";

/** Oturum durumu cookie'den okunur (istek başına dinamik); sayfanın geri kalanı statik kalabilir. */
async function AccountLinks() {
    if (await hasSession()) {
        return (
            <>
                <Button size="sm" className="hidden sm:inline-flex" asChild>
                    <Link href="/panel/etkinlik/yeni">
                        <Plus aria-hidden />
                        Etkinlik oluştur
                    </Link>
                </Button>
                <Button size="sm" variant="ghost" asChild>
                    <Link href="/hesabim">
                        <CircleUserRound aria-hidden />
                        Hesabım
                    </Link>
                </Button>
            </>
        );
    }
    return (
        <>
            <Button size="sm" variant="ghost" asChild>
                <Link href="/giris">Giriş yap</Link>
            </Button>
            <Button size="sm" asChild>
                <Link href="/kayit">Kayıt ol</Link>
            </Button>
        </>
    );
}

/**
 * Bütün sayfaların üst çubuğu: logo, ana menü, hesap. Sayfa kaydırılırken üstte kalır.
 * `children`: sayfaya özel ek bağlantılar (örn. tanıtım sayfasında "Nasıl çalışır").
 */
export function SiteHeader({ children }: { children?: React.ReactNode }) {
    return (
        <HeaderBar>
            <Suspense fallback={<MainNavFallback />}>
                <MainNav />
            </Suspense>
            <div className="ml-auto flex items-center gap-1 sm:gap-2">
                {children}
                <Suspense fallback={<span className="inline-block h-8 w-28" />}>
                    <AccountLinks />
                </Suspense>
            </div>
        </HeaderBar>
    );
}
