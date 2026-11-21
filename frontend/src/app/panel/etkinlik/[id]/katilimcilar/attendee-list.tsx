"use client";

import { Download, Search } from "lucide-react";
import { useMemo, useState } from "react";
import type { AttendeeResponse, FormField } from "@/api/generated/model";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ATTENDEE_STATUS, attendeesCsv, labeledAnswers } from "@/lib/attendees";
import { cn } from "@/lib/utils";

type Filter = "all" | "in" | "out";
const FILTERS: { value: Filter; label: string }[] = [
    { value: "all", label: "Tümü" },
    { value: "in", label: "Gelenler" },
    { value: "out", label: "Gelmeyenler" },
];

const time = new Intl.DateTimeFormat("tr-TR", {
    timeZone: "Europe/Istanbul",
    hour: "2-digit",
    minute: "2-digit",
});
const ACTIVE = new Set(["CONFIRMED", "CHECKED_IN"]);

export function AttendeeList({
    attendees,
    fields,
    title,
}: {
    attendees: AttendeeResponse[];
    fields: FormField[];
    title: string;
}) {
    const [query, setQuery] = useState("");
    const [filter, setFilter] = useState<Filter>("all");

    const registered = attendees.filter((a) => ACTIVE.has(a.status ?? "")).length;
    const checkedIn = attendees.filter((a) => a.status === "CHECKED_IN").length;
    const pending = attendees.filter((a) => a.status === "RESERVED").length;

    const shown = useMemo(() => {
        const q = query.trim().toLocaleLowerCase("tr");
        return attendees.filter((a) => {
            if (filter === "in" && a.status !== "CHECKED_IN") return false;
            if (filter === "out" && a.status !== "CONFIRMED") return false;
            return !q || `${a.name} ${a.email}`.toLocaleLowerCase("tr").includes(q);
        });
    }, [attendees, filter, query]);

    function download() {
        const blob = new Blob([attendeesCsv(attendees, fields)], {
            type: "text/csv;charset=utf-8",
        });
        const url = URL.createObjectURL(blob);
        const link = Object.assign(document.createElement("a"), {
            href: url,
            download: `${title} - katılımcılar.csv`,
        });
        link.click();
        URL.revokeObjectURL(url);
    }

    return (
        <div className="grid gap-6">
            <dl className="grid grid-cols-3 gap-3 sm:max-w-xl">
                {[
                    { label: "Kayıtlı", value: registered },
                    { label: "Girdi", value: checkedIn },
                    { label: "Onay bekliyor", value: pending },
                ].map((s) => (
                    <div key={s.label} className="rounded-xl border bg-card px-4 py-3">
                        <dt className="text-sm text-muted-foreground">{s.label}</dt>
                        <dd className="font-display text-3xl font-bold nums">{s.value}</dd>
                    </div>
                ))}
            </dl>

            <div className="flex flex-wrap items-center gap-3">
                <div className="relative min-w-56 flex-1 sm:max-w-sm">
                    <Search
                        className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground"
                        aria-hidden
                    />
                    <Input
                        type="search"
                        aria-label="İsim veya e-posta ara"
                        placeholder="İsim veya e-posta ara"
                        value={query}
                        onChange={(e) => setQuery(e.target.value)}
                        className="h-10 bg-card pl-9"
                    />
                </div>
                <div role="group" aria-label="Filtre" className="flex rounded-lg bg-muted p-1">
                    {FILTERS.map((f) => (
                        <button
                            key={f.value}
                            type="button"
                            aria-pressed={filter === f.value}
                            onClick={() => setFilter(f.value)}
                            className={cn(
                                "rounded-md px-3 py-1.5 text-sm font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50",
                                filter === f.value ? "bg-card shadow-xs" : "text-muted-foreground",
                            )}
                        >
                            {f.label}
                        </button>
                    ))}
                </div>
                <Button
                    variant="outline"
                    className="ml-auto"
                    onClick={download}
                    disabled={attendees.length === 0}
                >
                    <Download aria-hidden />
                    CSV indir
                </Button>
            </div>

            {attendees.length === 0 ? (
                <p className="rounded-xl border bg-card p-6 text-muted-foreground">
                    Henüz kayıt yok. Etkinlik yayındaysa bağlantısını paylaş; kayıtlar burada
                    görünecek.
                </p>
            ) : shown.length === 0 ? (
                <p className="text-muted-foreground">Aramana uyan katılımcı yok.</p>
            ) : (
                <ul className="divide-y rounded-xl border bg-card">
                    {shown.map((a) => {
                        const answers = labeledAnswers(a, fields);
                        return (
                            <li
                                key={a.registrationId}
                                className="grid gap-3 p-4 sm:grid-cols-[minmax(12rem,1fr)_8rem_2fr] sm:gap-6"
                            >
                                <div className="min-w-0">
                                    <p className="flex flex-wrap items-center gap-2 font-medium">
                                        {a.name}
                                        {a.guest && <Badge variant="outline">Misafir</Badge>}
                                    </p>
                                    <p className="truncate text-sm text-muted-foreground">
                                        {a.email}
                                    </p>
                                </div>
                                <div className="text-sm">
                                    <span
                                        className={cn(
                                            "font-medium",
                                            a.status === "CHECKED_IN" && "text-success",
                                            a.status === "CANCELLED" && "text-scarce",
                                        )}
                                    >
                                        {ATTENDEE_STATUS[a.status ?? ""] ?? a.status}
                                    </span>
                                    {a.checkedInAt && (
                                        <span className="block text-muted-foreground nums">
                                            {time.format(new Date(a.checkedInAt))}
                                        </span>
                                    )}
                                </div>
                                {answers.length > 0 ? (
                                    <dl className="grid gap-x-4 gap-y-1 text-sm sm:grid-cols-[auto_1fr]">
                                        {answers.map((ans) => (
                                            <div key={ans.label} className="contents">
                                                <dt className="text-muted-foreground">
                                                    {ans.label}
                                                </dt>
                                                <dd className="break-words">{ans.value}</dd>
                                            </div>
                                        ))}
                                    </dl>
                                ) : (
                                    <span className="hidden text-sm text-muted-foreground sm:block">
                                        Cevap yok
                                    </span>
                                )}
                            </li>
                        );
                    })}
                </ul>
            )}
        </div>
    );
}
