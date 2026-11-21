package com.venyu.registration.internal.web.dto;

import com.venyu.registration.internal.domain.Registration;
import com.venyu.registration.internal.domain.RegistrationStatus;
import com.venyu.registration.internal.service.RegistrationService.ManagedRegistration;

import java.time.Instant;
import java.util.UUID;

// Misafirin "yönet" sayfası: bileti (QR kodu) ve etkinliği gösterir.
public record ManagedRegistrationResponse(
    UUID id,
    String name,
    RegistrationStatus status,
    String ticketCode,
    String eventTitle,
    String eventSlug,
    Instant startsAt,
    Instant endsAt) {

    public static ManagedRegistrationResponse from(ManagedRegistration managed) {
        Registration registration = managed.registration();
        return new ManagedRegistrationResponse(registration.getId(), registration.getGuestName(),
            registration.getStatus(), registration.getTicketCode(), managed.event().title(), managed.event().slug(),
            managed.event().startsAt(), managed.event().endsAt());
    }
}
