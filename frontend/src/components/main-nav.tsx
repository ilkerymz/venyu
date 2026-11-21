"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

const LINKS = [{ href: "/mekanlar", label: "Mekanlar" }];

function NavLinks({ pathname }: { pathname?: string }) {
    return (
        <nav aria-label="Ana menü" className="flex items-center gap-1">
            {LINKS.map((link) => {
                const current =
                    pathname === link.href || pathname?.startsWith(`${link.href}/`) === true;
                return (
                    <Link
                        key={link.href}
                        href={link.href}
                        aria-current={current ? "page" : undefined}
                        className={cn(
                            "rounded-md px-3 py-1.5 text-sm font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50",
                            current
                                ? "bg-secondary text-secondary-foreground"
                                : "text-muted-foreground hover:bg-muted hover:text-foreground",
                        )}
                    >
                        {link.label}
                    </Link>
                );
            })}
        </nav>
    );
}

/** Ana menü; bulunduğun bölüm vurgulanır (adres istemcide okunur, Suspense içinde). */
export function MainNav() {
    return <NavLinks pathname={usePathname()} />;
}

/** Adres bilinmeden (prerender): vurgusuz aynı menü. */
export function MainNavFallback() {
    return <NavLinks />;
}
