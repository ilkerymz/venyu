import type { MyRegistrationResponse } from "@/api/generated/model";

/** Hesabım'daki "Katıldığım etkinlikler" kartının durumu. */
export type TicketState = "ready" | "checkedIn" | "pending" | "eventCancelled";

/**
 * Kartta ne yazacağı; `undefined` = listede gösterme (kullanıcının kendi iptal ettiği ya da süresi dolan kayıt).
 * Etkinlik iptal edildiyse kayıt durumundan bağımsız olarak bunu söyleriz.
 */
export function ticketState(registration: MyRegistrationResponse): TicketState | undefined {
    if (registration.event?.cancelled) return "eventCancelled";
    switch (registration.status) {
        case "CONFIRMED":
            return "ready";
        case "CHECKED_IN":
            return "checkedIn";
        case "RESERVED":
            return "pending";
        default:
            return undefined;
    }
}

/**
 * Gösterilecek kayıtlar, etkinlik başına bir tane. Backend en yeniden eskiye döner; aynı etkinliğe
 * iptal edip yeniden kayıt olunduysa en yeni kayıt kalır.
 */
export function visibleRegistrations(registrations: MyRegistrationResponse[]) {
    const seen = new Set<string>();
    const result: { registration: MyRegistrationResponse; state: TicketState }[] = [];
    for (const registration of registrations) {
        const state = ticketState(registration);
        const eventId = registration.eventId ?? "";
        if (!state || seen.has(eventId)) continue;
        seen.add(eventId);
        result.push({ registration, state });
    }
    return result;
}

/**
 * Kartın gideceği yer: etkinlik sayfası herkese açıksa orası; değilse (sadece davetli, iptal edilmiş)
 * sayfa katılımcıya 404 döner, o yüzden Hesabım'daki bilet sayfası.
 */
export function registrationHref(registration: MyRegistrationResponse): string {
    return registration.event?.publiclyVisible
        ? `/e/${registration.event.slug}`
        : `/hesabim/bilet/${registration.id}`;
}
