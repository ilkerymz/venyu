/** Davet formuna yapıştırılan metinden e-posta adresleri: virgül, noktalı virgül, boşluk ya da satırla ayrılmış. */
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function parseEmails(text: string): { valid: string[]; invalid: string[] } {
    const seen = new Set<string>();
    const valid: string[] = [];
    const invalid: string[] = [];
    for (const raw of text.split(/[\s,;]+/)) {
        const email = raw.trim().replace(/^<|>$/g, "").toLowerCase();
        if (!email || seen.has(email)) continue;
        seen.add(email);
        (EMAIL.test(email) && email.length <= 254 ? valid : invalid).push(email);
    }
    return { valid, invalid };
}
