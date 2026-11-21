package com.venyu.registration.internal.web.dto;

import com.venyu.registration.internal.domain.Registration;
import com.venyu.registration.internal.domain.RegistrationStatus;

import java.time.Instant;
import java.util.UUID;

// expiresAt: sadece onay bekleyen (RESERVED) misafir kaydında dolu.
public record RegistrationResponse(
    UUID id,
    UUID eventId,
    UUID ticketTypeId,
    RegistrationStatus status,
    Instant expiresAt,
    Instant createdAt) {

    public static RegistrationResponse from(Registration registration) {
        return new RegistrationResponse(registration.getId(), registration.getEventId(),
            registration.getTicketTypeId(), registration.getStatus(), registration.getExpiresAt(),
            registration.getCreatedAt());
    }
}
