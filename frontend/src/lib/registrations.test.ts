import { describe, expect, it } from "vitest";
import type { MyRegistrationResponse } from "@/api/generated/model";
import { registrationHref, ticketState, visibleRegistrations } from "./registrations";

const reg = (
    id: string,
    eventId: string,
    status: MyRegistrationResponse["status"],
    event: MyRegistrationResponse["event"] = { slug: "abc", publiclyVisible: true },
): MyRegistrationResponse => ({ id, eventId, status, event });

describe("ticketState", () => {
    it("kayıt durumunu kart durumuna çevirir", () => {
        expect(ticketState(reg("1", "e", "CONFIRMED"))).toBe("ready");
        expect(ticketState(reg("1", "e", "CHECKED_IN"))).toBe("checkedIn");
        expect(ticketState(reg("1", "e", "RESERVED"))).toBe("pending");
    });

    it("kullanıcının iptal ettiği ve süresi dolan kaydı gizler", () => {
        expect(ticketState(reg("1", "e", "CANCELLED"))).toBeUndefined();
        expect(ticketState(reg("1", "e", "EXPIRED"))).toBeUndefined();
    });

    it("etkinlik iptal edildiyse kayıt durumundan bağımsız olarak bunu söyler", () => {
        expect(ticketState(reg("1", "e", "CANCELLED", { cancelled: true }))).toBe("eventCancelled");
    });
});

describe("visibleRegistrations", () => {
    it("etkinlik başına en yeni görünür kaydı tutar", () => {
        const list = [
            reg("yeni", "e1", "CONFIRMED"),
            reg("eski", "e1", "CANCELLED"),
            reg("iptal", "e2", "CANCELLED"),
            reg("baska", "e3", "CHECKED_IN"),
        ];
        expect(visibleRegistrations(list).map((v) => v.registration.id)).toEqual(["yeni", "baska"]);
    });

    it("iptal edilen etkinlikte iki kayıt varsa bir kart kalır", () => {
        const list = [
            reg("yeni", "e1", "CANCELLED", { cancelled: true }),
            reg("eski", "e1", "CANCELLED", { cancelled: true }),
        ];
        expect(visibleRegistrations(list)).toHaveLength(1);
    });
});

describe("registrationHref", () => {
    it("herkese açık etkinlikte etkinlik sayfasına, değilse bilet sayfasına gider", () => {
        expect(registrationHref(reg("r1", "e", "CONFIRMED"))).toBe("/e/abc");
        expect(
            registrationHref(reg("r1", "e", "CONFIRMED", { slug: "abc", publiclyVisible: false })),
        ).toBe("/hesabim/bilet/r1");
    });
});
