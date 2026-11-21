package com.venyu.event.internal.web.dto;

import com.venyu.event.internal.domain.Event;
import com.venyu.event.internal.domain.EventStatus;
import com.venyu.event.internal.domain.EventVisibility;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
    UUID id,
    String slug,
    UUID organizerId,
    String title,
    String description,
    EventVisibility visibility,
    boolean requiresAccount,
    EventStatus status,
    UUID venueId,
    UUID venueBookingId,
    String customAddress,
    Instant startsAt,
    Instant endsAt,
    Instant publishedAt) {

    public static EventResponse from(Event event) {
        return new EventResponse(event.getId(), event.getSlug(), event.getOrganizerId(), event.getTitle(),
            event.getDescription(), event.getVisibility(), event.isRequiresAccount(), event.getStatus(),
            event.getVenueId(), event.getVenueBookingId(), event.getCustomAddress(), event.getStartsAt(),
            event.getEndsAt(), event.getPublishedAt());
    }
}
