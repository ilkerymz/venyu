import Link from "next/link";
import { Wordmark } from "@/components/wordmark";

const COLUMNS = [
    {
        title: "Keşfet",
        links: [
            { href: "/mekanlar", label: "Mekanlar" },
            { href: "/#nasil-calisir", label: "Nasıl çalışır" },
        ],
    },
    {
        title: "Düzenle",
        links: [
            { href: "/panel/etkinlik/yeni", label: "Etkinlik oluştur" },
            { href: "/hesabim", label: "Hesabım" },
        ],
    },
];

/** Bütün sayfaların altı (kök layout'ta). Statik: oturuma bakmaz, sayfayı dinamik yapmaz. */
export function SiteFooter() {
    return (
        <footer className="mt-auto border-t bg-card">
            <div className="mx-auto grid max-w-6xl grid-cols-2 gap-10 px-5 py-12 sm:px-8 md:grid-cols-[2fr_1fr_1fr]">
                <div className="col-span-2 md:col-span-1">
                    <Wordmark />
                    <p className="mt-3 max-w-xs text-sm leading-relaxed text-muted-foreground">
                        Mekanı bul, bileti sat, tek akışta. Mekanları, organizatörleri ve
                        katılımcıları buluşturan etkinlik platformu.
                    </p>
                </div>
                {COLUMNS.map((column) => (
                    <nav key={column.title} aria-label={column.title}>
                        <h2 className="text-sm font-semibold">{column.title}</h2>
                        <ul className="mt-3 grid gap-2">
                            {column.links.map((link) => (
                                <li key={link.href}>
                                    <Link
                                        href={link.href}
                                        className="rounded-sm text-sm text-muted-foreground underline-offset-4 outline-none hover:text-foreground hover:underline focus-visible:ring-[3px] focus-visible:ring-ring/50"
                                    >
                                        {link.label}
                                    </Link>
                                </li>
                            ))}
                        </ul>
                    </nav>
                ))}
            </div>
            <div className="border-t">
                <div className="mx-auto flex max-w-6xl flex-wrap justify-between gap-x-6 gap-y-1 px-5 py-5 text-xs text-muted-foreground sm:px-8">
                    <p>© 2026 Venyu</p>
                    <p>Bütün saatler İstanbul saatiyle gösterilir.</p>
                </div>
            </div>
        </footer>
    );
}
