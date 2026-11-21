import { describe, expect, it } from "vitest";
import type { AttendeeResponse, FormField } from "@/api/generated/model";
import { attendeesCsv, labeledAnswers } from "./attendees";

const fields: FormField[] = [
    { key: "soru_1", label: "Nasıl geleceksin?", type: "SINGLE_CHOICE", options: ["Araba"] },
    { key: "soru_2", label: "Günler", type: "MULTI_CHOICE", options: ["Cmt", "Paz"] },
];
const attendee: AttendeeResponse = {
    name: 'Ayşe "Ay" Yılmaz',
    email: "ayse@ornek.com",
    guest: true,
    status: "CHECKED_IN",
    checkedInAt: "2026-12-06T06:10:00Z",
    answers: { soru_1: "Araba", soru_2: ["Cmt", "Paz"], eski: true },
};

describe("labeledAnswers", () => {
    it("cevapları soru metniyle eşler, silinmiş soruyu işaretler", () => {
        expect(labeledAnswers(attendee, fields)).toEqual([
            { label: "Nasıl geleceksin?", value: "Araba" },
            { label: "Günler", value: "Cmt, Paz" },
            { label: "Silinmiş soru", value: "Evet" },
        ]);
    });
});

describe("attendeesCsv", () => {
    it("BOM, noktalı virgül ve tırnak kaçışıyla Excel uyumlu CSV üretir", () => {
        const csv = attendeesCsv([attendee], fields);
        expect(csv.startsWith("﻿")).toBe(true);
        const [header, row] = csv.slice(1).split("\r\n");
        expect(header).toBe('"Ad";"E-posta";"Tür";"Durum";"Giriş";"Nasıl geleceksin?";"Günler"');
        expect(row).toContain('"Ayşe ""Ay"" Yılmaz";"ayse@ornek.com";"Misafir";"Girdi"');
        expect(row.endsWith('"Araba";"Cmt, Paz"')).toBe(true);
    });

    it("formül gibi başlayan değerleri etkisizleştirir", () => {
        const csv = attendeesCsv([{ ...attendee, name: "=HYPERLINK(1)" }], fields);
        expect(csv).toContain(`"'=HYPERLINK(1)"`);
    });
});
