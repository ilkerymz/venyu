import type { FormField } from "@/api/generated/model";

/**
 * Form verisini backend'in beklediği cevap tiplerine çevirir (ADR-009):
 * TEXT/LONG_TEXT/SINGLE_CHOICE → string, NUMBER → number, BOOLEAN → true, MULTI_CHOICE → string[].
 * Boş alanlar gönderilmez.
 */
export function readAnswers(fields: FormField[], form: FormData): Record<string, unknown> {
    const answers: Record<string, unknown> = {};
    for (const field of fields) {
        const key = field.key;
        if (field.type === "MULTI_CHOICE") {
            const values = form.getAll(key).map(String);
            if (values.length) answers[key] = values;
        } else if (field.type === "BOOLEAN") {
            if (form.get(key) === "on") answers[key] = true;
        } else {
            const value = String(form.get(key) ?? "").trim();
            if (!value) continue;
            answers[key] = field.type === "NUMBER" ? Number(value.replace(",", ".")) : value;
        }
    }
    return answers;
}

/** Backend hata satırı "anahtar: mesaj" biçiminde; kullanıcıya sorunun kendisini göster. */
export function labelErrors(errors: string[], fields: FormField[]): string[] {
    const labels = new Map(fields.map((f) => [f.key, f.label]));
    return errors.map((error) => {
        const match = /^([a-z][a-z0-9_]*): (.*)$/.exec(error);
        const label = match && labels.get(match[1]);
        return label ? `${label}: ${match[2]}` : error;
    });
}
