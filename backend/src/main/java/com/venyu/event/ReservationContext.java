package com.venyu.event;

/**
 * Koltuğu kimin, hangi yoldan istediği. "Hesap gerekli" ve "sadece davetliler" kuralları
 * envanterin sahibi event modülünde uygulanır (ADR-007).
 */
public record ReservationContext(boolean asGuest, boolean withInvitation) {

    public static ReservationContext member() {
        return new ReservationContext(false, false);
    }

    public static ReservationContext guest() {
        return new ReservationContext(true, false);
    }

    public static ReservationContext invitedMember() {
        return new ReservationContext(false, true);
    }

    public static ReservationContext invitedGuest() {
        return new ReservationContext(true, true);
    }
}
