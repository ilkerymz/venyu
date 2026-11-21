"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

/** Şeritteki sekmeler: etkinlik sayfası + yönetim sekmeleri. Bulunulan sekme açık renkle vurgulanır. */
export function OrganizerNav({ eventId, slug }: { eventId: string; slug: string }) {
    const pathname = usePathname();
    const base = `/panel/etkinlik/${eventId}`;
    const links = [
        { href: `/e/${slug}`, label: "Etkinlik sayfası" },
        { href: base, label: "Genel" },
        { href: `${base}/katilimcilar`, label: "Katılımcılar" },
        { href: `${base}/davetler`, label: "Davetler" },
        { href: `${base}/form`, label: "Kayıt formu" },
        { href: `${base}/kapi`, label: "Kapı" },
    ];
    return (
        <nav aria-label="Yönetim" className="-mx-1 flex max-w-full gap-1 overflow-x-auto">
            {links.map((link) => {
                const current = pathname === link.href;
                return (
                    <Link
                        key={link.href}
                        href={link.href}
                        aria-current={current ? "page" : undefined}
                        className={cn(
                            "rounded-md px-3 py-1.5 text-sm font-medium whitespace-nowrap outline-none focus-visible:ring-[3px] focus-visible:ring-background/50",
                            current
                                ? "bg-background text-foreground"
                                : "text-background/75 hover:bg-background/10 hover:text-background",
                        )}
                    >
                        {link.label}
                    </Link>
                );
            })}
        </nav>
    );
}
