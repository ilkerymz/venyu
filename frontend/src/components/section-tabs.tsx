"use client";

import Link from "next/link";
import { useParams, usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

type Tab = { href: string; label: string };

/** `[id]` segmentli yönetim sayfalarının sekmeleri: `${prefix}/${id}${tab.href}`. */
export function SectionTabs({
    prefix,
    tabs,
    label,
}: {
    prefix: string;
    tabs: Tab[];
    label: string;
}) {
    const { id } = useParams<{ id: string }>();
    const pathname = usePathname();
    const base = `${prefix}/${id}`;
    return (
        <nav
            aria-label={label}
            className="-mx-5 mt-6 overflow-x-auto border-b px-5 sm:mx-0 sm:px-0"
        >
            <ul className="flex gap-1">
                {tabs.map((tab) => {
                    const href = base + tab.href;
                    const current = pathname === href;
                    return (
                        <li key={tab.href}>
                            <Link
                                href={href}
                                aria-current={current ? "page" : undefined}
                                className={cn(
                                    "-mb-px block rounded-t-md border-b-2 px-3 py-2.5 text-sm font-medium whitespace-nowrap outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50",
                                    current
                                        ? "border-primary text-foreground"
                                        : "border-transparent text-muted-foreground hover:text-foreground",
                                )}
                            >
                                {tab.label}
                            </Link>
                        </li>
                    );
                })}
            </ul>
        </nav>
    );
}
