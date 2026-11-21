import type { Metadata } from "next";
import "@fontsource-variable/onest";
import "@fontsource-variable/bricolage-grotesque/standard.css";
import "./globals.css";
import { SiteFooter } from "@/components/site-footer";

export const metadata: Metadata = {
    title: {
        default: "Venyu",
        template: "%s | Venyu",
    },
    description:
        "Mekanı bul, bileti sat, tek akışta. Mekanları, organizatörleri ve katılımcıları buluşturan etkinlik platformu.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
    return (
        <html lang="tr">
            {/* Sütun düzeni: alt bilgi kısa sayfalarda da en altta. *:w-full: mx-auto ile ortalanan
                bölümler flex içinde içeriğe göre daralmasın. */}
            <body className="flex min-h-dvh flex-col *:w-full">
                {children}
                <SiteFooter />
            </body>
        </html>
    );
}
