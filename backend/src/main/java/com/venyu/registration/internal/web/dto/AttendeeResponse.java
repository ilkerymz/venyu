package com.venyu.registration.internal.web.dto;

import com.venyu.registration.internal.domain.Registration;
import com.venyu.registration.internal.domain.RegistrationStatus;
import com.venyu.registration.internal.service.AttendeeService.Attendee;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

// Organizatörün gördüğü katılımcı satırı. Bilet kodu yok: kod kapıda okutulur, listede gerekmez.
public record AttendeeResponse(
    UUID registrationId,
    String name,
    String email,
    boolean guest,
    UUID ticketTypeId,
    RegistrationStatus status,
    Instant checkedInAt,
    Integer formVersion,
    Map<String, Object> answers,
    Instant createdAt) {

    public static AttendeeResponse from(Attendee attendee) {
        Registration registration = attendee.registration();
        return new AttendeeResponse(registration.getId(), attendee.name(), attendee.email(),
            registration.getUserId() == null, registration.getTicketTypeId(), registration.getStatus(),
            registration.getCheckedInAt(), registration.getFormVersion(), attendee.answers(),
            registration.getCreatedAt());
    }
}
