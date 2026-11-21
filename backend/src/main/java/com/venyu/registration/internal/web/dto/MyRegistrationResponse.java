package com.venyu.registration.internal.web.dto;

import com.venyu.event.EventSummary;
import com.venyu.registration.internal.domain.Registration;
import com.venyu.registration.internal.domain.RegistrationStatus;
import com.venyu.registration.internal.service.RegistrationService.MyRegistration;

import java.time.Instant;
import java.util.UUID;

// GET /me/registrations: kayıt + etkinliğin kart için gereken bilgisi (Hesabım'daki "Katıldığım etkinlikler").
// Kayıt alanları RegistrationResponse ile aynı adlarda (frontend'in mevcut kullanımı bozulmasın).
public record MyRegistrationResponse(
    UUID id,
    UUID eventId,
    UUID ticketTypeId,
    RegistrationStatus status,
    String ticketCode,
    Integer formVersion,
    Instant expiresAt,
    Instant checkedInAt,
    Instant createdAt,
    RegisteredEvent event) {

    // publiclyVisible=false (sadece davetli): etkinlik sayfası katılımcıya 404 döner, frontend bileti başka yerde gösterir.
    public record RegisteredEvent(
        String title,
        String slug,
        Instant startsAt,
        Instant endsAt,
        UUID venueId,
        String customAddress,
        boolean cancelled,
        boolean publiclyVisible) {
    }

    public static MyRegistrationResponse from(MyRegistration mine) {
        Registration registration = mine.registration();
        EventSummary event = mine.event();
        return new MyRegistrationResponse(registration.getId(), registration.getEventId(),
            registration.getTicketTypeId(), registration.getStatus(), registration.getTicketCode(),
            registration.getFormVersion(), registration.getExpiresAt(), registration.getCheckedInAt(),
            registration.getCreatedAt(),
            new RegisteredEvent(event.title(), event.slug(), event.startsAt(), event.endsAt(), event.venueId(),
                event.customAddress(), event.cancelled(), event.publiclyVisible()));
    }
}
