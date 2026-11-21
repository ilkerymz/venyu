package com.venyu.event.internal.web.dto;

import com.venyu.event.internal.domain.EventDetails;
import com.venyu.event.internal.domain.EventLocation;
import com.venyu.event.internal.domain.EventVisibility;
import com.venyu.event.internal.exception.InvalidEventException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

// Konum: ya venueBookingId ya (customAddress + startsAt + endsAt). Zaman mekanlıda rezervasyondan gelir.
public record CreateEventRequest(
    @NotBlank @Size(max = 150) String title,
    @Size(max = 5000) String description,
    @NotNull EventVisibility visibility,
    UUID venueBookingId,
    @Size(max = 300) String customAddress,
    OffsetDateTime startsAt,
    OffsetDateTime endsAt) {

    public EventDetails toDetails() {
        return new EventDetails(title, description, visibility);
    }

    public EventLocation toLocation() {
        boolean addressGiven = customAddress != null || startsAt != null || endsAt != null;
        if (venueBookingId != null && !addressGiven) {
            return new EventLocation.AtVenue(venueBookingId);
        }
        if (venueBookingId == null && customAddress != null && !customAddress.isBlank()
            && startsAt != null && endsAt != null) {
            return new EventLocation.AtAddress(customAddress, startsAt.toInstant(), endsAt.toInstant());
        }
        throw new InvalidEventException(
            "Konum için ya venueBookingId ya da customAddress + startsAt + endsAt gönderilmeli.");
    }
}
