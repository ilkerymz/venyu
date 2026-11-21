package com.venyu.registration.internal.web.dto;

import com.venyu.registration.internal.domain.Registration;
import com.venyu.registration.internal.domain.RegistrationStatus;

import java.time.Instant;
import java.util.UUID;

// ticketCode: QR'a basılacak kod, sadece onaylı kayıtta dolu. expiresAt: sadece onay bekleyen misafir kaydında.
// formVersion: formsuz etkinlikte null.
public record RegistrationResponse(
    UUID id,
    UUID eventId,
    UUID ticketTypeId,
    RegistrationStatus status,
    String ticketCode,
    Integer formVersion,
    Instant expiresAt,
    Instant checkedInAt,
    Instant createdAt) {

    public static RegistrationResponse from(Registration registration) {
        return new RegistrationResponse(registration.getId(), registration.getEventId(),
            registration.getTicketTypeId(), registration.getStatus(), registration.getTicketCode(),
            registration.getFormVersion(), registration.getExpiresAt(), registration.getCheckedInAt(),
            registration.getCreatedAt());
    }
}
