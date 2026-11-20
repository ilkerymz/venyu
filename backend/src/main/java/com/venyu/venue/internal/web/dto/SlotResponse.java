package com.venyu.venue.internal.web.dto;

import com.venyu.venue.internal.domain.VenueSlot;

import java.time.Instant;
import java.util.UUID;

public record SlotResponse(UUID id, UUID venueId, Instant startsAt, Instant endsAt) {

    public static SlotResponse from(VenueSlot slot) {
        return new SlotResponse(slot.getId(), slot.getVenueId(), slot.getStartsAt(), slot.getEndsAt());
    }
}