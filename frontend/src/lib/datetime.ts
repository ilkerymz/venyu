/**
 * Formdaki tarih ve saat alanlarını backend'in beklediği ISO zamana çevirir.
 * Organizatör saati İstanbul saatiyle girer; Türkiye 2016'dan beri sabit UTC+3 (yaz saati yok).
 * Bitiş saati başlangıçtan önce ya da ona eşitse ertesi gün sayılır (22:00–02:00 gibi gece etkinlikleri).
 */
const OFFSET = "+03:00";

function nextDay(date: string): string {
    const d = new Date(`${date}T00:00:00Z`);
    d.setUTCDate(d.getUTCDate() + 1);
    return d.toISOString().slice(0, 10);
}

export function eventRange(
    date: string,
    startTime: string,
    endTime: string,
): { startsAt: string; endsAt: string; overnight: boolean } {
    const overnight = endTime <= startTime;
    const endDate = overnight ? nextDay(date) : date;
    return {
        startsAt: `${date}T${startTime}:00${OFFSET}`,
        endsAt: `${endDate}T${endTime}:00${OFFSET}`,
        overnight,
    };
}

type Timed = { startsAt?: string; endsAt?: string };

/**
 * Listeler için: bitişi geçmemiş olanlar yaklaşan (en yakını önce), bitmiş olanlar geçmiş
 * (en yenisi önce). Süren etkinlik yaklaşanlarda kalır.
 */
export function splitByTime<T>(
    items: T[],
    timeOf: (item: T) => Timed,
    now: number,
): { upcoming: T[]; past: T[] } {
    const isPast = (item: T) => Date.parse(timeOf(item).endsAt ?? "") < now;
    const byStart = (a: T, b: T) =>
        (timeOf(a).startsAt ?? "").localeCompare(timeOf(b).startsAt ?? "");
    return {
        upcoming: items.filter((item) => !isPast(item)).sort(byStart),
        past: items.filter(isPast).sort((a, b) => byStart(b, a)),
    };
}
