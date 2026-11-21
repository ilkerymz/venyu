import { describe, expect, it } from "vitest";
import {
    buildShowIf,
    type FieldDraft,
    fromFields,
    nextKey,
    parseShowIf,
    pruneConditions,
    toFields,
} from "./form-builder";
import { visibleFieldKeys } from "./form-visibility";

const draft = (over: Partial<FieldDraft>): FieldDraft => ({
    key: "k",
    label: "L",
    type: "TEXT",
    required: false,
    optionsText: "",
    ticketTypeIds: [],
    ...over,
});

describe("nextKey", () => {
    it("boştaki ilk soru_N anahtarını verir", () => {
        expect(nextKey([])).toBe("soru_1");
        expect(nextKey(["soru_1", "soru_3"])).toBe("soru_2");
    });
});

describe("showIf üretme ve okuma", () => {
    it("soru tipine göre ifade üretir", () => {
        expect(buildShowIf({ key: "ulasim", op: "==", value: "Araba" }, "SINGLE_CHOICE")).toBe(
            "answers.ulasim == 'Araba'",
        );
        expect(buildShowIf({ key: "vegan", op: "==", value: "true" }, "BOOLEAN")).toBe(
            "answers.vegan == true",
        );
        expect(buildShowIf({ key: "yil", op: ">=", value: "2" }, "NUMBER")).toBe(
            "answers.yil >= 2",
        );
    });

    it("tırnak içeren seçeneği kaçışlar ve geri okur", () => {
        const expr = buildShowIf({ key: "yer", op: "==", value: "Ali'nin evi" }, "SINGLE_CHOICE");
        expect(expr).toBe("answers.yer == 'Ali\\'nin evi'");
        expect(parseShowIf(expr)).toEqual({ key: "yer", op: "==", value: "Ali'nin evi" });
    });

    it("üretilen ifade tarayıcıdaki görünürlük hesabıyla çalışır", () => {
        const fields = toFields([
            draft({
                key: "yer",
                label: "Yer",
                type: "SINGLE_CHOICE",
                optionsText: "Ali'nin evi\nPark",
            }),
            draft({
                key: "adres",
                label: "Adres",
                condition: { key: "yer", op: "==", value: "Ali'nin evi" },
            }),
        ]);
        expect(visibleFieldKeys(fields, { yer: "Ali'nin evi" }, "t").has("adres")).toBe(true);
        expect(visibleFieldKeys(fields, { yer: "Park" }, "t").has("adres")).toBe(false);
    });

    it("çözemediği koşulu ham haliyle korur", () => {
        const [d] = fromFields([
            { key: "a", label: "A", type: "TEXT", showIf: "answers.x == 1 && ticketTypeId == 'y'" },
        ]);
        expect(d.condition).toBeUndefined();
        expect(toFields([d])[0].showIf).toBe("answers.x == 1 && ticketTypeId == 'y'");
    });
});

describe("toFields", () => {
    it("seçenekleri sadece seçenekli alanlara yazar, boş satırları atar", () => {
        const [choice, text] = toFields([
            draft({ key: "a", type: "MULTI_CHOICE", optionsText: "Cmt\n\n Paz \n" }),
            draft({ key: "b", type: "TEXT", optionsText: "unutulmuş" }),
        ]);
        expect(choice.options).toEqual(["Cmt", "Paz"]);
        expect(text.options).toBeUndefined();
    });
});

describe("pruneConditions", () => {
    it("sonraki ya da silinmiş soruya bakan koşulu temizler", () => {
        const pruned = pruneConditions([
            draft({ key: "a", condition: { key: "b", op: "==", value: "x" } }),
            draft({ key: "b", type: "SINGLE_CHOICE" }),
            draft({ key: "c", condition: { key: "b", op: "==", value: "x" } }),
            draft({ key: "d", condition: { key: "silindi", op: "==", value: "x" } }),
        ]);
        expect(pruned.map((d) => d.condition?.key)).toEqual([undefined, undefined, "b", undefined]);
    });
});
