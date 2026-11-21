import { describe, expect, it } from "vitest";
import type { FormField } from "@/api/generated/model";
import { visibleFieldKeys } from "./form-visibility";

const fields: FormField[] = [
    {
        key: "transport",
        label: "Nasıl geleceksin?",
        type: "SINGLE_CHOICE",
        options: ["car", "bus"],
    },
    { key: "plate", label: "Plaka", type: "TEXT", showIf: "answers.transport == 'car'" },
    { key: "experience", label: "Kaç yıllık deneyim?", type: "NUMBER" },
    {
        key: "advanced",
        label: "İleri seviye atölye?",
        type: "BOOLEAN",
        showIf: "answers.experience >= 2",
    },
    { key: "meal", label: "Yemek tercihi", type: "TEXT", ticketTypeIds: ["vip"] },
    { key: "broken", label: "Bozuk ifade", type: "TEXT", showIf: "answers.(" },
];

describe("visibleFieldKeys", () => {
    it("showIf önceki cevaba göre alanı açar ve kapatır", () => {
        expect(visibleFieldKeys(fields, { transport: "car" }, "std").has("plate")).toBe(true);
        expect(visibleFieldKeys(fields, { transport: "bus" }, "std").has("plate")).toBe(false);
    });

    it("boş ya da cevaplanmamış sayı alanıyla karşılaştırmada alanı gizler", () => {
        expect(visibleFieldKeys(fields, {}, "std").has("advanced")).toBe(false);
        expect(visibleFieldKeys(fields, { experience: 3 }, "std").has("advanced")).toBe(true);
    });

    it("gizli alanın cevabı sonraki ifadelerde null sayılır", () => {
        const chained: FormField[] = [
            { key: "a", label: "A", type: "TEXT", showIf: "ticketTypeId == 'vip'" },
            { key: "b", label: "B", type: "TEXT", showIf: "answers.a == 'x'" },
        ];
        expect(visibleFieldKeys(chained, { a: "x" }, "std").has("b")).toBe(false);
        expect(visibleFieldKeys(chained, { a: "x" }, "vip").has("b")).toBe(true);
    });

    it("bilete özel alanı sadece o bilette gösterir", () => {
        expect(visibleFieldKeys(fields, {}, "vip").has("meal")).toBe(true);
        expect(visibleFieldKeys(fields, {}, "std").has("meal")).toBe(false);
    });

    it("hatalı ifadede alanı gizler", () => {
        expect(visibleFieldKeys(fields, {}, "std").has("broken")).toBe(false);
    });
});
