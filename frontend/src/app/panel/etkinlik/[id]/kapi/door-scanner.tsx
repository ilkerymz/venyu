"use client";

import { Camera, CameraOff } from "lucide-react";
import type QrScanner from "qr-scanner";
import { useEffect, useRef, useState, useTransition } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { type CheckInResult, checkInAction } from "../../../actions";

/** Aynı kod bu süre içinde tekrar okunursa yok sayılır (kamera aynı QR'ı saniyede birkaç kez görür). */
const REPEAT_WINDOW_MS = 4000;
const time = new Intl.DateTimeFormat("tr-TR", {
    timeZone: "Europe/Istanbul",
    hour: "2-digit",
    minute: "2-digit",
});

type Props = { eventId: string; checkedIn: number; registered: number };

export function DoorScanner({ eventId, checkedIn: initialIn, registered }: Props) {
    const videoRef = useRef<HTMLVideoElement>(null);
    const scannerRef = useRef<QrScanner | null>(null);
    const lastRef = useRef<{ code: string; at: number } | null>(null);
    const [cameraOn, setCameraOn] = useState(false);
    const [cameraError, setCameraError] = useState<string>();
    const [result, setResult] = useState<CheckInResult>();
    const [checkedIn, setCheckedIn] = useState(initialIn);
    const [manual, setManual] = useState("");
    const [pending, startTransition] = useTransition();

    function submit(code: string) {
        const now = Date.now();
        const last = lastRef.current;
        if (!code || (last && last.code === code && now - last.at < REPEAT_WINDOW_MS)) return;
        lastRef.current = { code, at: now };
        startTransition(async () => {
            const r = await checkInAction(eventId, code);
            setResult(r);
            if (r.kind === "ok") setCheckedIn((n) => n + 1);
            navigator.vibrate?.(r.kind === "ok" ? 80 : [60, 60, 60]);
        });
    }
    const submitRef = useRef(submit);
    useEffect(() => {
        submitRef.current = submit;
    });

    async function startCamera() {
        setCameraError(undefined);
        try {
            const { default: Scanner } = await import("qr-scanner");
            if (!(await Scanner.hasCamera())) {
                setCameraError("Bu cihazda kamera bulunamadı. Kodu elle girebilirsin.");
                return;
            }
            const scanner = new Scanner(videoRef.current!, (r) => submitRef.current(r.data), {
                preferredCamera: "environment",
                highlightScanRegion: true,
                returnDetailedScanResult: true,
            });
            scannerRef.current = scanner;
            await scanner.start();
            setCameraOn(true);
        } catch {
            setCameraError(
                "Kamera açılamadı. Tarayıcıya kamera izni ver; telefonda deniyorsan sayfa HTTPS üzerinden açılmalı.",
            );
        }
    }

    function stopCamera() {
        scannerRef.current?.destroy();
        scannerRef.current = null;
        setCameraOn(false);
    }

    useEffect(() => () => scannerRef.current?.destroy(), []);

    return (
        <div className="mx-auto grid max-w-md gap-5">
            <p className="text-center">
                <span className="font-display text-5xl font-bold nums">{checkedIn}</span>
                <span className="text-xl text-muted-foreground nums"> / {registered} giriş</span>
            </p>

            <ResultBanner result={result} pending={pending} />

            <div
                className={cn(
                    "relative aspect-square overflow-hidden rounded-xl bg-black",
                    !cameraOn && "hidden",
                )}
            >
                <video ref={videoRef} className="size-full object-cover" muted playsInline />
            </div>
            {cameraOn ? (
                <Button variant="outline" onClick={stopCamera}>
                    <CameraOff aria-hidden />
                    Kamerayı kapat
                </Button>
            ) : (
                <Button size="lg" className="h-12" onClick={startCamera}>
                    <Camera aria-hidden />
                    Kamerayla okut
                </Button>
            )}
            {cameraError && <p className="text-sm text-destructive">{cameraError}</p>}

            <form
                className="grid gap-2"
                onSubmit={(e) => {
                    e.preventDefault();
                    lastRef.current = null; // elle girişte tekrar penceresi uygulanmaz
                    submit(manual.trim());
                    setManual("");
                }}
            >
                <label htmlFor="ticket-code" className="text-sm font-medium">
                    Ya da bilet kodunu yaz
                </label>
                <div className="flex gap-2">
                    <Input
                        id="ticket-code"
                        value={manual}
                        onChange={(e) => setManual(e.target.value)}
                        autoComplete="off"
                        autoCapitalize="off"
                        spellCheck={false}
                        maxLength={64}
                        className="h-11 bg-card font-mono"
                    />
                    <Button type="submit" className="h-11" disabled={!manual.trim() || pending}>
                        Giriş yap
                    </Button>
                </div>
            </form>
        </div>
    );
}

function ResultBanner({ result, pending }: { result?: CheckInResult; pending: boolean }) {
    if (pending) {
        return (
            <div className="rounded-xl bg-muted px-5 py-6 text-center font-medium" role="status">
                Kontrol ediliyor…
            </div>
        );
    }
    if (!result) {
        return (
            <div
                className="rounded-xl bg-muted px-5 py-6 text-center text-muted-foreground"
                role="status"
            >
                Bileti okut ya da kodu gir.
            </div>
        );
    }
    const style = {
        ok: "bg-success text-white",
        already: "bg-warning text-white",
        invalid: "bg-destructive text-white",
    }[result.kind];
    return (
        <div
            role="status"
            aria-live="assertive"
            className={cn("rounded-xl px-5 py-6 text-center", style)}
        >
            {result.kind === "ok" && (
                <>
                    <p className="font-display text-3xl font-bold">{result.name}</p>
                    <p className="mt-1">Giriş yapıldı</p>
                </>
            )}
            {result.kind === "already" && (
                <>
                    <p className="font-display text-3xl font-bold">Zaten girdi</p>
                    {result.at && (
                        <p className="mt-1 nums">
                            {time.format(new Date(result.at))} saatinde okutulmuş
                        </p>
                    )}
                </>
            )}
            {result.kind === "invalid" && (
                <>
                    <p className="font-display text-3xl font-bold">Geçersiz bilet</p>
                    <p className="mt-1">{result.message}</p>
                </>
            )}
        </div>
    );
}
