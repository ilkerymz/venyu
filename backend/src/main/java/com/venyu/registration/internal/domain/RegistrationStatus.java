package com.venyu.registration.internal.domain;

import java.util.Set;

public enum RegistrationStatus {
    RESERVED,          // koltuk tutuluyor, süreli: misafir e-posta onayı (F2-4), F3'te ödeme
    PENDING_THRESHOLD, // F3: ödeme alındı (pre-auth), eşik bekleniyor
    CONFIRMED,
    CHECKED_IN,        // F2-6
    EXPIRED,           // RESERVED süresi doldu, koltuk bırakıldı
    REFUNDED,
    CANCELLED;

    // registrations_one_active_per_user / _per_guest index'leriyle aynı olmalı.
    public static final Set<RegistrationStatus> ACTIVE = Set.of(RESERVED, PENDING_THRESHOLD, CONFIRMED, CHECKED_IN);

    // Etkinlik iptal edilince iptal edilenler. CHECKED_IN değişmez (giriş olmuş bir gerçek);
    // PENDING_THRESHOLD F3'te iadeyle birlikte eklenecek.
    public static final Set<RegistrationStatus> CANCELLED_WITH_EVENT = Set.of(RESERVED, CONFIRMED);
}
