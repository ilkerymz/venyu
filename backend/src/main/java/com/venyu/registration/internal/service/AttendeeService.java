package com.venyu.registration.internal.service;

import com.venyu.event.EventSummary;
import com.venyu.identity.UserContact;
import com.venyu.identity.UserDirectory;
import com.venyu.registration.internal.domain.Registration;
import com.venyu.registration.internal.domain.RegistrationStatus;
import com.venyu.registration.internal.exception.AlreadyCheckedInException;
import com.venyu.registration.internal.exception.RegistrationStateException;
import com.venyu.registration.internal.exception.TicketNotFoundException;
import com.venyu.registration.internal.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// Organizatörün katılımcı listesi ve kapıda check-in (ADR-010).
@Service
@RequiredArgsConstructor
public class AttendeeService {

    // Kapı etkinlikten 3 saat önce açılır, etkinlik bitince kapanır.
    static final Duration CHECK_IN_OPENS_BEFORE = Duration.ofHours(3);

    private final RegistrationRepository registrations;
    private final OrganizerEvents organizerEvents;
    private final UserDirectory userDirectory;
    private final AnswersJson answersJson;

    @Transactional(readOnly = true)
    public List<Attendee> list(UUID organizerId, UUID eventId) {
        organizerEvents.require(organizerId, eventId);
        List<Registration> list = registrations
            .findByEventIdAndStatusNotOrderByCreatedAtAsc(eventId, RegistrationStatus.EXPIRED);
        // Üyelerin ad ve e-postası identity'de: tek toplu sorgu (N+1 yok).
        Map<UUID, UserContact> members = userDirectory.findContacts(
            list.stream().map(Registration::getUserId).filter(Objects::nonNull).distinct().toList());
        return list.stream().map(registration -> toAttendee(registration, members)).toList();
    }

    @Transactional
    public Attendee checkIn(UUID organizerId, UUID eventId, String ticketCode) {
        EventSummary event = organizerEvents.require(organizerId, eventId);
        Instant now = Instant.now();
        if (now.isBefore(event.startsAt().minus(CHECK_IN_OPENS_BEFORE)) || now.isAfter(event.endsAt())) {
            throw new RegistrationStateException(
                "Giriş, etkinlikten " + CHECK_IN_OPENS_BEFORE.toHours() + " saat önce başlar ve etkinlik bitince kapanır.");
        }
        boolean checkedIn = registrations.checkIn(eventId, ticketCode, organizerId, now) == 1;
        // UPDATE'ten sonra okuma: başarılıysa güncel satır, değilse nedeni (yok / zaten girmiş / iptal).
        Registration registration = registrations.findByEventIdAndTicketCode(eventId, ticketCode)
            .orElseThrow(TicketNotFoundException::new);
        if (!checkedIn) {
            if (registration.getStatus() == RegistrationStatus.CHECKED_IN) {
                throw new AlreadyCheckedInException(registration.getCheckedInAt());
            }
            throw new RegistrationStateException("Bilet geçerli değil (durum: " + registration.getStatus() + ").");
        }
        return toAttendee(registration, userDirectory.findContacts(
            registration.getUserId() == null ? List.of() : List.of(registration.getUserId())));
    }

    /**
     * Listedeki bir satır. Üyenin hesabı silinmişse ad/e-posta null olabilir (modüller arası FK yok, ADR-006).
     */
    public record Attendee(Registration registration, String name, String email, Map<String, Object> answers) {
    }

    private Attendee toAttendee(Registration registration, Map<UUID, UserContact> members) {
        Map<String, Object> answers = answersJson.read(registration.getAnswers());
        if (registration.getUserId() == null) {
            return new Attendee(registration, registration.getGuestName(), registration.getGuestEmail(), answers);
        }
        UserContact contact = members.get(registration.getUserId());
        return contact == null
            ? new Attendee(registration, null, null, answers)
            : new Attendee(registration, contact.displayName(), contact.email(), answers);
    }
}
