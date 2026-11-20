package com.venyu.venue.internal.web.dto;

import com.venyu.venue.internal.domain.VenueSlot;
import com.venyu.venue.internal.service.VenueSlotService;

import java.time.Instant;
import java.util.UUID;

public record SlotResponse(UUID id, UUID venueId, Instant startsAt, Instant endsAt, boolean available) {

    public static SlotResponse from(VenueSlotService.SlotAvailability availability) {
        VenueSlot slot = availability.slot();
        return new SlotResponse(slot.getId(), slot.getVenueId(), slot.getStartsAt(), slot.getEndsAt(),
                availability.available());
    }

    // Yeni açılan slot her zaman müsait.
    public static SlotResponse from(VenueSlot slot) {
        return new SlotResponse(slot.getId(), slot.getVenueId(), slot.getStartsAt(), slot.getEndsAt(), true);
    }
}