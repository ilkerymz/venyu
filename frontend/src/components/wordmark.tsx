import Link from "next/link";
import { cn } from "@/lib/utils";

export function Wordmark({ className }: { className?: string }) {
    return (
        <Link
            href="/"
            className={cn(
                "font-display text-2xl font-extrabold tracking-tight [font-stretch:80%]",
                "rounded-sm outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50",
                className,
            )}
        >
            venyu
        </Link>
    );
}
