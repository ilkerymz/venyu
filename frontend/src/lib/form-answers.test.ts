import { describe, expect, it } from "vitest";
import type { FormField } from "@/api/generated/model";
import { labelErrors, readAnswers } from "./form-answers";

const fields: FormField[] = [
    { key: "name", label: "Takma ad", type: "TEXT" },
    { key: "age", label: "Yaş", type: "NUMBER" },
    { key: "vegan", label: "Vegan mısın?", type: "BOOLEAN" },
    { key: "days", label: "Günler", type: "MULTI_CHOICE", options: ["cmt", "paz"] },
    { key: "note", label: "Not", type: "LONG_TEXT" },
];

describe("readAnswers", () => {
    it("alan tiplerine göre çevirir ve boşları atlar", () => {
        const form = new FormData();
        form.set("name", "  Ayşe ");
        form.set("age", "27,5");
        form.set("vegan", "on");
        form.append("days", "cmt");
        form.append("days", "paz");
        form.set("note", "   ");
        expect(readAnswers(fields, form)).toEqual({
            name: "Ayşe",
            age: 27.5,
            vegan: true,
            days: ["cmt", "paz"],
        });
    });
});

describe("labelErrors", () => {
    it("backend'in alan anahtarını sorunun etiketiyle değiştirir", () => {
        expect(labelErrors(["age: sayı olmalı.", "Genel hata."], fields)).toEqual([
            "Yaş: sayı olmalı.",
            "Genel hata.",
        ]);
    });
});
