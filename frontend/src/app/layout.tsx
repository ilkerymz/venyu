import type { Metadata } from "next";
import "@fontsource-variable/onest";
import "@fontsource-variable/bricolage-grotesque/standard.css";
import "./globals.css";

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
            <body className="min-h-dvh">{children}</body>
        </html>
    );
}
