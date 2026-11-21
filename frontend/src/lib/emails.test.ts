import { describe, expect, it } from "vitest";
import { parseEmails } from "./emails";

describe("parseEmails", () => {
    it("farklı ayırıcıları, tekrarları ve büyük harfleri temizler", () => {
        const text =
            "Ayse@Ornek.com, ali@ornek.com\nayse@ornek.com; <can@ornek.com.tr>  hatali@ yok";
        expect(parseEmails(text)).toEqual({
            valid: ["ayse@ornek.com", "ali@ornek.com", "can@ornek.com.tr"],
            invalid: ["hatali@", "yok"],
        });
    });
});
