package com.venyu.event;

import java.time.Instant;
import java.util.UUID;

public record EventSummary(UUID id, String title, String slug, UUID organizerId, Instant startsAt, Instant endsAt,
                           boolean requiresAccount) {

    public boolean isOrganizedBy(UUID userId) {
        return organizerId.equals(userId);
    }
}
