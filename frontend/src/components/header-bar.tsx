import { Wordmark } from "@/components/wordmark";

/**
 * Üst çubuğun iskeleti (logo + yer). `SiteHeader` bunu menü ve hesapla doldurur; giriş sayfaları ve
 * hata ekranları sade haliyle kullanır (aynı yükseklik, aynı çizgi: sayfa geçişinde zıplamaz).
 */
export function HeaderBar({ children }: { children?: React.ReactNode }) {
    return (
        <header className="sticky top-0 z-40 border-b border-border/70 bg-background/85 backdrop-blur-md supports-[backdrop-filter]:bg-background/70">
            <div className="mx-auto flex h-16 max-w-6xl items-center gap-6 px-5 sm:px-8">
                <Wordmark />
                {children}
            </div>
        </header>
    );
}
