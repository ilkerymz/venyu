package com.venyu.event;

import java.time.Instant;
import java.util.UUID;

// publiclyVisible: yayında ve INVITE_ONLY değil (organizatör dışındakiler görebilir).
// Konum: mekanlı etkinlikte venueId, adresli etkinlikte customAddress dolu (ADR-007).
public record EventSummary(UUID id, String title, String slug, UUID organizerId, Instant startsAt, Instant endsAt,
                           boolean requiresAccount, boolean publiclyVisible, UUID venueId, String customAddress,
                           boolean cancelled) {

    public boolean isOrganizedBy(UUID userId) {
        return organizerId.equals(userId);
    }
}
