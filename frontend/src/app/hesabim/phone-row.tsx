"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { PhoneVerification } from "./phone-verification";

/** Doğrulanmamış telefon: "Doğrula" kartın içinde kod formunu açar. */
export function PhoneRow({ current }: { current?: string }) {
    const [open, setOpen] = useState(false);
    return (
        <>
            <div className="flex items-center justify-between gap-3">
                <span className="text-sm text-muted-foreground">
                    {current ? "Doğrulanmadı" : "Eklenmedi"}
                </span>
                <Button
                    size="sm"
                    variant={open ? "ghost" : "outline"}
                    aria-expanded={open}
                    aria-controls="phone-verification"
                    onClick={() => setOpen(!open)}
                >
                    {open ? "Vazgeç" : "Doğrula"}
                </Button>
            </div>
            {open && (
                <div id="phone-verification" className="mt-4">
                    <p className="mb-4 text-sm leading-relaxed text-muted-foreground">
                        Ücretli etkinliklere bilet almak ve ücretli etkinlik düzenlemek için
                        gerekli.
                    </p>
                    <PhoneVerification current={current} />
                </div>
            )}
        </>
    );
}
