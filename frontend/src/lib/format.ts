/**
 * Ekranda gösterilen tarih ve para biçimleri.
 * Backend zamanları UTC `Instant` olarak döner; kullanıcıya her zaman
 * etkinliğin şehri olan İstanbul saatiyle gösterilir (tarayıcının saat
 * diliminden bağımsız, sunucu ve istemci aynı metni üretir).
 */
const LOCALE = "tr-TR";
const TIME_ZONE = "Europe/Istanbul";

const dayFormat = new Intl.DateTimeFormat(LOCALE, {
    timeZone: TIME_ZONE,
    day: "numeric",
    month: "long",
    weekday: "long",
});

const timeFormat = new Intl.DateTimeFormat(LOCALE, {
    timeZone: TIME_ZONE,
    hour: "2-digit",
    minute: "2-digit",
});

const partsFormat = new Intl.DateTimeFormat(LOCALE, {
    timeZone: TIME_ZONE,
    day: "numeric",
    month: "short",
    weekday: "short",
});

/** "25 Kasım Salı" */
export function formatEventDay(iso: string): string {
    return dayFormat.format(new Date(iso));
}

/** "19:30–22:00" */
export function formatTimeRange(startIso: string, endIso: string): string {
    return `${timeFormat.format(new Date(startIso))}–${timeFormat.format(new Date(endIso))}`;
}

/** Bilet üzerindeki tarih bloğu için parçalar: { day: "25", month: "Kas", weekday: "Sal" } */
export function eventDateParts(iso: string): { day: string; month: string; weekday: string } {
    const parts = partsFormat.formatToParts(new Date(iso));
    const pick = (type: Intl.DateTimeFormatPartTypes) =>
        parts.find((p) => p.type === type)?.value ?? "";
    return { day: pick("day"), month: pick("month"), weekday: pick("weekday") };
}

/** 0 → "Ücretsiz", 250 → "₺250", 249.5 → "₺249,50" */
export function formatPrice(amount: number, currency: string): string {
    if (amount === 0) return "Ücretsiz";
    return new Intl.NumberFormat(LOCALE, {
        style: "currency",
        currency,
        minimumFractionDigits: Number.isInteger(amount) ? 0 : 2,
        maximumFractionDigits: 2,
    }).format(amount);
}
