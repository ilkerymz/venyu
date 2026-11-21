"use client";

import { ArrowDown, ArrowUp, Plus, Trash2 } from "lucide-react";
import { startTransition, useActionState, useState } from "react";
import type { FormFieldType, TicketTypeResponse } from "@/api/generated/model";
import { FormErrors, Notice } from "@/components/form-parts";
import { QuestionField } from "@/components/form-question";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { readAnswers } from "@/lib/form-answers";
import {
    CHOICE_TYPES,
    CONDITION_SOURCE_TYPES,
    type ConditionOp,
    type FieldDraft,
    nextKey,
    optionsOf,
    pruneConditions,
    toFields,
} from "@/lib/form-builder";
import { visibleFieldKeys } from "@/lib/form-visibility";
import { saveFormAction } from "../../../actions";

const TYPE_LABELS: Record<FormFieldType, string> = {
    TEXT: "Kısa metin",
    LONG_TEXT: "Uzun metin",
    NUMBER: "Sayı",
    BOOLEAN: "Onay kutusu",
    SINGLE_CHOICE: "Tek seçim",
    MULTI_CHOICE: "Çoklu seçim",
};
/** Seçim ve onay kutusu: "[cevap] ise / değilse". Sayı: "[en az] [3] ise". */
const CHOICE_OPS: Partial<Record<ConditionOp, string>> = { "==": "ise", "!=": "değilse" };
const NUMBER_OPS: Record<ConditionOp, string> = {
    "==": "tam olarak",
    "!=": "farklı",
    ">=": "en az",
    "<=": "en fazla",
    ">": "fazla",
    "<": "az",
};
const select =
    "border-input bg-card h-9 rounded-md border px-2 text-sm outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50";

type Props = { eventId: string; initial: FieldDraft[]; tickets: TicketTypeResponse[] };

export function FormBuilder({ eventId, initial, tickets }: Props) {
    const [drafts, setDrafts] = useState(initial);
    const [state, dispatch, pending] = useActionState(saveFormAction.bind(null, eventId), {});
    const [localErrors, setLocalErrors] = useState<string[]>([]);

    const update = (index: number, change: Partial<FieldDraft>) =>
        setDrafts((ds) =>
            pruneConditions(ds.map((d, i) => (i === index ? { ...d, ...change } : d))),
        );
    const move = (index: number, delta: number) =>
        setDrafts((ds) => {
            const next = [...ds];
            [next[index], next[index + delta]] = [next[index + delta], next[index]];
            return pruneConditions(next);
        });
    const remove = (index: number) =>
        setDrafts((ds) => pruneConditions(ds.filter((_, i) => i !== index)));
    const add = () =>
        setDrafts((ds) => [
            ...ds,
            {
                key: nextKey(ds.map((d) => d.key)),
                label: "",
                type: "TEXT",
                required: false,
                optionsText: "",
                ticketTypeIds: [],
            },
        ]);

    function save() {
        const errors = drafts.flatMap((d, i) => [
            ...(d.label.trim() ? [] : [`${i + 1}. sorunun metni boş.`]),
            ...(CHOICE_TYPES.includes(d.type) && optionsOf(d).length === 0
                ? [`${i + 1}. soru için en az bir seçenek yaz.`]
                : []),
        ]);
        setLocalErrors(errors);
        if (errors.length === 0) startTransition(() => dispatch(toFields(drafts)));
    }

    return (
        <div className="grid gap-8 lg:grid-cols-[1fr_20rem]">
            <div className="grid content-start gap-4">
                {drafts.length === 0 && (
                    <p className="rounded-xl border bg-card p-6 leading-relaxed text-muted-foreground">
                        Bu etkinlikte kayıt formu yok; katılımcılar sadece bilet seçip kayıt olur.
                        Örneğin ulaşım, yemek tercihi ya da deneyim seviyesi sormak için soru ekle.
                    </p>
                )}
                {drafts.map((draft, i) => (
                    <QuestionEditor
                        key={draft.key}
                        index={i}
                        draft={draft}
                        earlier={drafts
                            .slice(0, i)
                            .filter((d) => CONDITION_SOURCE_TYPES.includes(d.type))}
                        tickets={tickets}
                        isLast={i === drafts.length - 1}
                        onChange={(change) => update(i, change)}
                        onMove={(delta) => move(i, delta)}
                        onRemove={() => remove(i)}
                    />
                ))}
                <Button
                    variant="outline"
                    className="justify-self-start"
                    onClick={add}
                    disabled={drafts.length >= 50}
                >
                    <Plus aria-hidden />
                    Soru ekle
                </Button>

                <div className="mt-4 grid gap-3 border-t pt-6">
                    <FormErrors errors={localErrors.length ? localErrors : state.errors} />
                    {state.done && !pending && (
                        <Notice>Form kaydedildi. Yeni kayıtlar bu soruları görecek.</Notice>
                    )}
                    <Button
                        size="lg"
                        className="h-11 justify-self-start"
                        onClick={save}
                        disabled={pending}
                    >
                        {pending ? "Kaydediliyor…" : "Formu kaydet"}
                    </Button>
                    <p className="text-sm text-muted-foreground">
                        Kaydettiğinde formun yeni bir sürümü oluşur; önceki kayıtların cevapları
                        kendi sürümleriyle saklanır.
                    </p>
                </div>
            </div>

            <Preview drafts={drafts} tickets={tickets} />
        </div>
    );
}

type EditorProps = {
    index: number;
    draft: FieldDraft;
    earlier: FieldDraft[];
    tickets: TicketTypeResponse[];
    isLast: boolean;
    onChange: (change: Partial<FieldDraft>) => void;
    onMove: (delta: number) => void;
    onRemove: () => void;
};

function QuestionEditor({
    index,
    draft,
    earlier,
    tickets,
    isLast,
    onChange,
    onMove,
    onRemove,
}: EditorProps) {
    const id = `f-${draft.key}`;
    const isChoice = CHOICE_TYPES.includes(draft.type);
    return (
        <fieldset className="grid gap-4 rounded-xl border bg-card p-5">
            <legend className="sr-only">{index + 1}. soru</legend>
            <div className="flex items-start gap-3">
                <span className="pt-2 font-display text-sm font-semibold text-primary nums">
                    {index + 1}
                </span>
                <div className="grid flex-1 gap-2">
                    <Label htmlFor={`${id}-label`} className="sr-only">
                        Soru
                    </Label>
                    <Input
                        id={`${id}-label`}
                        value={draft.label}
                        maxLength={200}
                        placeholder="Soruyu yaz, örn. Kabataş'a nasıl geleceksin?"
                        className="h-10 bg-card font-medium"
                        onChange={(e) => onChange({ label: e.target.value })}
                    />
                </div>
                <div className="flex shrink-0">
                    <Button
                        variant="ghost"
                        size="icon-sm"
                        aria-label="Yukarı taşı"
                        disabled={index === 0}
                        onClick={() => onMove(-1)}
                    >
                        <ArrowUp aria-hidden />
                    </Button>
                    <Button
                        variant="ghost"
                        size="icon-sm"
                        aria-label="Aşağı taşı"
                        disabled={isLast}
                        onClick={() => onMove(1)}
                    >
                        <ArrowDown aria-hidden />
                    </Button>
                    <Button
                        variant="ghost"
                        size="icon-sm"
                        aria-label="Soruyu sil"
                        className="text-destructive"
                        onClick={onRemove}
                    >
                        <Trash2 aria-hidden />
                    </Button>
                </div>
            </div>

            <div className="flex flex-wrap items-center gap-x-6 gap-y-3 pl-6">
                <label className="flex items-center gap-2 text-sm">
                    Cevap tipi
                    <select
                        className={select}
                        value={draft.type}
                        onChange={(e) => onChange({ type: e.target.value as FormFieldType })}
                    >
                        {Object.entries(TYPE_LABELS).map(([value, label]) => (
                            <option key={value} value={value}>
                                {label}
                            </option>
                        ))}
                    </select>
                </label>
                <label className="flex items-center gap-2 text-sm">
                    <input
                        type="checkbox"
                        checked={draft.required}
                        onChange={(e) => onChange({ required: e.target.checked })}
                        className="size-4 accent-primary"
                    />
                    {draft.type === "BOOLEAN" ? "İşaretlemek zorunlu" : "Zorunlu"}
                </label>
            </div>

            {isChoice && (
                <div className="grid gap-2 pl-6">
                    <Label htmlFor={`${id}-options`}>Seçenekler</Label>
                    <Textarea
                        id={`${id}-options`}
                        value={draft.optionsText}
                        onChange={(e) => onChange({ optionsText: e.target.value })}
                        placeholder={"Her satıra bir seçenek\nAraba\nToplu taşıma"}
                        className="min-h-24 bg-card"
                    />
                </div>
            )}

            <ConditionEditor draft={draft} earlier={earlier} onChange={onChange} />

            {tickets.length > 1 && (
                <fieldset className="grid gap-2 pl-6">
                    <legend className="mb-1 text-sm text-muted-foreground">
                        Sadece şu biletleri seçenlere sor (hiçbiri işaretli değilse herkese sorulur)
                    </legend>
                    <div className="flex flex-wrap gap-x-5 gap-y-2">
                        {tickets.map((t) => (
                            <label key={t.id} className="flex items-center gap-2 text-sm">
                                <input
                                    type="checkbox"
                                    className="size-4 accent-primary"
                                    checked={draft.ticketTypeIds.includes(t.id ?? "")}
                                    onChange={(e) =>
                                        onChange({
                                            ticketTypeIds: e.target.checked
                                                ? [...draft.ticketTypeIds, t.id ?? ""]
                                                : draft.ticketTypeIds.filter((x) => x !== t.id),
                                        })
                                    }
                                />
                                {t.name}
                            </label>
                        ))}
                    </div>
                </fieldset>
            )}
        </fieldset>
    );
}

function ConditionEditor({
    draft,
    earlier,
    onChange,
}: {
    draft: FieldDraft;
    earlier: FieldDraft[];
    onChange: (change: Partial<FieldDraft>) => void;
}) {
    if (draft.rawShowIf) {
        return (
            <div className="flex flex-wrap items-center gap-2 pl-6 text-sm">
                <span className="text-muted-foreground">Özel koşul:</span>
                <code className="rounded bg-muted px-2 py-1 text-xs">{draft.rawShowIf}</code>
                <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => onChange({ rawShowIf: undefined })}
                >
                    Koşulu kaldır
                </Button>
            </div>
        );
    }
    if (!draft.condition) {
        return earlier.length ? (
            <Button
                variant="ghost"
                size="sm"
                className="justify-self-start pl-6 text-primary"
                onClick={() =>
                    onChange({
                        condition: {
                            key: earlier.at(-1)?.key ?? "",
                            op: "==",
                            value: firstValue(earlier.at(-1)),
                        },
                    })
                }
            >
                Koşul ekle: önceki bir cevaba göre göster
            </Button>
        ) : null;
    }

    const source = earlier.find((d) => d.key === draft.condition?.key) ?? earlier[0];
    const cond = draft.condition;
    const isNumber = source?.type === "NUMBER";
    const set = (change: Partial<typeof cond>) => onChange({ condition: { ...cond, ...change } });
    const opSelect = (labels: Partial<Record<ConditionOp, string>>) => (
        <select
            aria-label="Koşul"
            className={select}
            value={cond.op}
            onChange={(e) => set({ op: e.target.value as ConditionOp })}
        >
            {Object.entries(labels).map(([op, label]) => (
                <option key={op} value={op}>
                    {label}
                </option>
            ))}
        </select>
    );

    return (
        <div className="flex flex-wrap items-center gap-2 rounded-lg bg-secondary/40 py-3 pr-3 pl-6 text-sm">
            <span>Sadece</span>
            <select
                aria-label="Hangi soru"
                className={`${select} max-w-60`}
                value={cond.key}
                onChange={(e) => {
                    const next = earlier.find((d) => d.key === e.target.value);
                    set({ key: e.target.value, op: "==", value: firstValue(next) });
                }}
            >
                {earlier.map((d) => (
                    <option key={d.key} value={d.key}>
                        {d.label || "(metinsiz soru)"}
                    </option>
                ))}
            </select>
            <span>cevabı</span>
            {isNumber && opSelect(NUMBER_OPS)}
            {source?.type === "SINGLE_CHOICE" ? (
                <select
                    aria-label="Cevap"
                    className={select}
                    value={cond.value}
                    onChange={(e) => set({ value: e.target.value })}
                >
                    {optionsOf(source).map((o) => (
                        <option key={o} value={o}>
                            {o}
                        </option>
                    ))}
                </select>
            ) : source?.type === "BOOLEAN" ? (
                <select
                    aria-label="Cevap"
                    className={select}
                    value={cond.value}
                    onChange={(e) => set({ value: e.target.value })}
                >
                    <option value="true">işaretli</option>
                    <option value="false">işaretsiz</option>
                </select>
            ) : (
                <Input
                    aria-label="Sayı"
                    type="number"
                    className="h-9 w-20 bg-card"
                    value={cond.value}
                    onChange={(e) => set({ value: e.target.value })}
                />
            )}
            {isNumber ? <span>ise</span> : opSelect(CHOICE_OPS)}
            <span>sor.</span>
            <Button
                variant="ghost"
                size="sm"
                className="ml-auto"
                onClick={() => onChange({ condition: undefined })}
            >
                Koşulu kaldır
            </Button>
        </div>
    );
}

function firstValue(source?: FieldDraft): string {
    if (!source) return "";
    if (source.type === "BOOLEAN") return "true";
    if (source.type === "NUMBER") return "1";
    return optionsOf(source)[0] ?? "";
}

/** Katılımcının göreceği hali; koşullar burada da canlı çalışır. */
function Preview({ drafts, tickets }: { drafts: FieldDraft[]; tickets: TicketTypeResponse[] }) {
    const fields = toFields(drafts).filter((f) => f.label);
    const [answers, setAnswers] = useState<Record<string, unknown>>({});
    const [ticketTypeId, setTicketTypeId] = useState(tickets[0]?.id ?? "");
    const visible = visibleFieldKeys(fields, answers, ticketTypeId);

    return (
        <aside className="lg:sticky lg:top-8 lg:self-start" aria-labelledby="preview-heading">
            <div className="rounded-xl border bg-card p-5">
                <h2 id="preview-heading" className="text-lg font-semibold">
                    Önizleme
                </h2>
                <p className="mt-1 text-sm text-muted-foreground">
                    Katılımcı formu böyle görür. Cevap verip koşulları dene.
                </p>
                {tickets.length > 1 && (
                    <label className="mt-4 flex items-center gap-2 text-sm">
                        Bilet
                        <select
                            className={select}
                            value={ticketTypeId}
                            onChange={(e) => setTicketTypeId(e.target.value)}
                        >
                            {tickets.map((t) => (
                                <option key={t.id} value={t.id}>
                                    {t.name}
                                </option>
                            ))}
                        </select>
                    </label>
                )}
                <form
                    className="mt-5 grid gap-5"
                    onChange={(e) => setAnswers(readAnswers(fields, new FormData(e.currentTarget)))}
                    onSubmit={(e) => e.preventDefault()}
                >
                    {fields
                        .filter((f) => visible.has(f.key))
                        .map((f) => (
                            <QuestionField key={f.key} field={f} />
                        ))}
                    {fields.length === 0 && (
                        <p className="text-sm text-muted-foreground">Henüz soru yok.</p>
                    )}
                </form>
            </div>
        </aside>
    );
}
