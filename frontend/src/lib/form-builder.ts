import type { FormField, FormFieldType } from "@/api/generated/model";

/**
 * Form oluşturucunun saf mantığı: organizatör JEXL yazmaz; "şu soru şu cevapsa göster"
 * seçiminden ifade üretilir (ADR-009, F2 raporu §11). Üretilen ifadeler sadece sembol
 * operatörleri kullanır; böylece Java JEXL ile tarayıcıdaki jexl aynı sonucu verir.
 */
export type ConditionOp = "==" | "!=" | ">" | ">=" | "<" | "<=";

export type Condition = { key: string; op: ConditionOp; value: string };

export type FieldDraft = {
    key: string;
    label: string;
    type: FormFieldType;
    required: boolean;
    /** Seçenekli alanlarda her satır bir seçenek. */
    optionsText: string;
    ticketTypeIds: string[];
    condition?: Condition;
    /** Oluşturucunun çözemediği (elle yazılmış) koşul olduğu gibi korunur. */
    rawShowIf?: string;
};

export const CHOICE_TYPES: FormFieldType[] = ["SINGLE_CHOICE", "MULTI_CHOICE"];
/** Koşulda kullanılabilecek önceki soru tipleri. */
export const CONDITION_SOURCE_TYPES: FormFieldType[] = ["SINGLE_CHOICE", "BOOLEAN", "NUMBER"];

/**
 * Yeni soruya kalıcı bir anahtar: `soru_1`, `soru_2`… Anahtar sorunun metninden türetilmez;
 * böylece metin değişince koşullar ve eski kayıtların cevapları bozulmaz.
 */
export function nextKey(taken: Iterable<string>): string {
    const used = new Set(taken);
    let i = 1;
    while (used.has(`soru_${i}`)) i++;
    return `soru_${i}`;
}

const quote = (value: string) => `'${value.replace(/\\/g, "\\\\").replace(/'/g, "\\'")}'`;

export function buildShowIf(condition: Condition, sourceType: FormFieldType | undefined): string {
    const left = `answers.${condition.key}`;
    if (sourceType === "BOOLEAN") return `${left} ${condition.op} ${condition.value === "true"}`;
    if (sourceType === "NUMBER")
        return `${left} ${condition.op} ${Number(condition.value.replace(",", ".")) || 0}`;
    return `${left} ${condition.op} ${quote(condition.value)}`;
}

const SHOW_IF =
    /^answers\.([a-z][a-z0-9_]*) (==|!=|>=|<=|>|<) (true|false|-?\d+(?:\.\d+)?|'(?:[^'\\]|\\.)*')$/;

export function parseShowIf(expression: string): Condition | undefined {
    const match = SHOW_IF.exec(expression.trim());
    if (!match) return undefined;
    const [, key, op, raw] = match;
    const value = raw.startsWith("'") ? raw.slice(1, -1).replace(/\\(.)/g, "$1") : raw;
    return { key, op: op as ConditionOp, value };
}

export function fromFields(fields: FormField[]): FieldDraft[] {
    return fields.map((f) => {
        const condition = f.showIf ? parseShowIf(f.showIf) : undefined;
        return {
            key: f.key,
            label: f.label,
            type: f.type,
            required: f.required ?? false,
            optionsText: (f.options ?? []).join("\n"),
            ticketTypeIds: f.ticketTypeIds ?? [],
            condition,
            rawShowIf: f.showIf && !condition ? f.showIf : undefined,
        };
    });
}

export function optionsOf(draft: FieldDraft): string[] {
    return draft.optionsText
        .split("\n")
        .map((o) => o.trim())
        .filter(Boolean);
}

export function toFields(drafts: FieldDraft[]): FormField[] {
    return drafts.map((d) => {
        const source = d.condition && drafts.find((x) => x.key === d.condition?.key);
        const showIf = d.condition ? buildShowIf(d.condition, source?.type) : d.rawShowIf;
        return {
            key: d.key,
            label: d.label.trim(),
            type: d.type,
            required: d.required,
            options: CHOICE_TYPES.includes(d.type) ? optionsOf(d) : undefined,
            ticketTypeIds: d.ticketTypeIds.length ? d.ticketTypeIds : undefined,
            showIf: showIf || undefined,
        };
    });
}

/**
 * Bir soru silindiğinde, taşındığında ya da tipi değiştiğinde artık geçersiz kalan koşulları
 * temizler: koşul sadece kendinden önceki, koşula uygun tipteki bir soruya bakabilir.
 */
export function pruneConditions(drafts: FieldDraft[]): FieldDraft[] {
    return drafts.map((d, i) => {
        if (!d.condition) return d;
        const source = drafts.slice(0, i).find((x) => x.key === d.condition?.key);
        return source && CONDITION_SOURCE_TYPES.includes(source.type)
            ? d
            : { ...d, condition: undefined };
    });
}
