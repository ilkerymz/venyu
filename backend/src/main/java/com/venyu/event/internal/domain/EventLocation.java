package com.venyu.event.internal.domain;

import java.time.Instant;
import java.util.UUID;

// Etkinliğin yeri: ya Venyu mekanındaki onaylı rezervasyon ya serbest adres (ADR-007).
public sealed interface EventLocation {

    record AtVenue(UUID bookingId) implements EventLocation {
    }

    record AtAddress(String address, Instant startsAt, Instant endsAt) implements EventLocation {
    }
}
