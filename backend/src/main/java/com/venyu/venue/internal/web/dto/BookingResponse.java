package com.venyu.venue.internal.web.dto;

import com.venyu.venue.internal.domain.BookingStatus;
import com.venyu.venue.internal.domain.VenueBooking;

import java.time.Instant;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID venueId,
        UUID slotId,
        UUID organizerId,
        BookingStatus status,
        String note,
        Instant expiresAt) {

    public static BookingResponse from(VenueBooking booking) {
        return new BookingResponse(booking.getId(), booking.getVenueId(), booking.getSlotId(),
                booking.getOrganizerId(), booking.getStatus(), booking.getNote(), booking.getExpiresAt());
    }
}