/**
 * Telefon numarası: kullanıcı "0532 123 45 67", "532 123 4567" ya da "+90 532…" yazabilir;
 * backend E.164 bekler (`^\+[1-9]\d{7,14}$`). Başında + yoksa Türkiye numarası sayılır.
 */
const E164 = /^\+[1-9]\d{7,14}$/;

export function normalizePhone(input: string): string | undefined {
    const compact = input.replace(/[\s\-().]/g, "");
    let phone = compact;
    if (compact.startsWith("00")) phone = `+${compact.slice(2)}`;
    else if (!compact.startsWith("+")) {
        const national = compact.replace(/^0/, "");
        phone = `+90${national}`;
        if (national.length !== 10) return undefined;
    }
    return E164.test(phone) ? phone : undefined;
}

/** +905321234567 → +90 532 123 45 67 (diğer ülkeler olduğu gibi). */
export function formatPhone(phone: string): string {
    const tr = /^\+90(\d{3})(\d{3})(\d{2})(\d{2})$/.exec(phone);
    return tr ? `+90 ${tr[1]} ${tr[2]} ${tr[3]} ${tr[4]}` : phone;
}
