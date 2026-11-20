package com.venyu.venue.internal.domain;

import java.util.Set;

public enum BookingStatus {
    HELD,
    CONFIRMED,
    REJECTED,
    EXPIRED,
    COMPLETED;

    // venue_bookings_one_active_per_slot index'indeki durumlarla aynı olmalı.
    public static final Set<BookingStatus> ACTIVE = Set.of(HELD, CONFIRMED);
}