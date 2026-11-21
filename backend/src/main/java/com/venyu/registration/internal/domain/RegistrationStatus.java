package com.venyu.registration.internal.domain;

import java.util.Set;

public enum RegistrationStatus {
    RESERVED,          // F3: ödeme bekleniyor
    PENDING_THRESHOLD, // F3: ödeme alındı (pre-auth), eşik bekleniyor
    CONFIRMED,
    CHECKED_IN,        // F2-6
    EXPIRED,
    REFUNDED,
    CANCELLED;

    // registrations_one_active_per_user index'iyle aynı olmalı.
    public static final Set<RegistrationStatus> ACTIVE = Set.of(RESERVED, PENDING_THRESHOLD, CONFIRMED, CHECKED_IN);
}
