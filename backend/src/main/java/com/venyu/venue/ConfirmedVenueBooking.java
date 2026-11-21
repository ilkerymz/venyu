package com.venyu.venue;

import java.time.Instant;
import java.util.UUID;

public record ConfirmedVenueBooking(
    UUID bookingId,
    UUID venueId,
    String venueName,
    int venueCapacity,
    Instant startsAt,
    Instant endsAt) {
}
