import jexl from "jexl";
import type { FormField } from "@/api/generated/model";

/**
 * Kayıt formunda hangi soruların görüneceğini backend'deki kuralla aynı şekilde hesaplar
 * (form modülü, `EventFormsService.validate` + `FormRules.isVisible`, ADR-009):
 * - Alanlar sırayla değerlendirilir; `showIf` sadece önceki alanların cevaplarını görür.
 * - Gizli ya da boş alanın değeri ifadede `null` sayılır.
 * - `ticketTypeIds` doluysa alan sadece o biletlerde görünür.
 * - İfade hata verirse alan gizlenir.
 * Bu hesap sadece kullanıcı deneyimi içindir; karar her zaman sunucuda yeniden verilir.
 */
export type Answers = Record<string, unknown>;

export function isEmptyAnswer(value: unknown): boolean {
    return (
        value === undefined ||
        value === null ||
        (typeof value === "string" && value.trim() === "") ||
        (Array.isArray(value) && value.length === 0)
    );
}

function evaluate(expression: string, answers: Answers, ticketTypeId: string): boolean {
    try {
        return jexl.evalSync(expression, { answers, ticketTypeId }) === true;
    } catch {
        return false;
    }
}

export function visibleFieldKeys(
    fields: FormField[],
    answers: Answers,
    ticketTypeId: string,
): Set<string> {
    const seen: Answers = {};
    const visible = new Set<string>();
    for (const field of fields) {
        const key = field.key ?? "";
        seen[key] = null;
        const ticketIds = field.ticketTypeIds ?? [];
        if (ticketIds.length > 0 && !ticketIds.includes(ticketTypeId)) continue;
        if (field.showIf && !evaluate(field.showIf, seen, ticketTypeId)) continue;
        visible.add(key);
        if (!isEmptyAnswer(answers[key])) seen[key] = answers[key];
    }
    return visible;
}
