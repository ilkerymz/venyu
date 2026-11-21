package com.venyu.registration.internal.service;

import com.venyu.event.EventLookup;
import com.venyu.event.EventSummary;
import com.venyu.event.ReservationContext;
import com.venyu.event.TicketOffer;
import com.venyu.form.EventForm;
import com.venyu.form.EventForms;
import com.venyu.registration.InvitationSent;
import com.venyu.registration.internal.domain.Invitation;
import com.venyu.registration.internal.domain.Registration;
import com.venyu.registration.internal.exception.EventOrganizerOnlyException;
import com.venyu.registration.internal.exception.InvalidInvitationTokenException;
import com.venyu.registration.internal.exception.InvitationConflictException;
import com.venyu.registration.internal.exception.RegistrationNotAvailableException;
import com.venyu.registration.internal.exception.RegistrationTargetNotFoundException;
import com.venyu.registration.internal.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitations;
    private final RegistrationService registrationService;
    private final EventLookup eventLookup;
    private final EventForms eventForms;
    private final ApplicationEventPublisher events;

    // Zaten davet edilmiş e-postalar sessizce atlanır (idempotent); dönen liste sadece yeni davetler.
    @Transactional
    public List<Invitation> invite(UUID organizerId, UUID eventId, List<String> emails) {
        EventSummary event = ownedEvent(organizerId, eventId);
        if (!event.startsAt().isAfter(Instant.now())) {
            throw new RegistrationNotAvailableException("Etkinlik başladı veya sona erdi.");
        }
        Set<String> requested = emails.stream().map(Emails::normalize)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> alreadyInvited = new HashSet<>(invitations.findExistingEmails(eventId, requested));

        List<Invitation> created = new ArrayList<>();
        for (String email : requested) {
            if (alreadyInvited.contains(email)) {
                continue;
            }
            String token = SecureTokens.generate();
            created.add(Invitation.create(eventId, email, SecureTokens.hash(token), organizerId));
            // Outbox: aşağıdaki flush patlarsa transaction geri alınır, e-postalar da gitmez.
            events.publishEvent(new InvitationSent(email, event.title(), token));
        }
        try {
            return invitations.saveAllAndFlush(created);
        } catch (DataIntegrityViolationException e) {
            // Aynı e-postaya eşzamanlı ikinci davet isteği (invitations_event_email_idx).
            throw new InvitationConflictException();
        }
    }

    @Transactional(readOnly = true)
    public List<Invitation> list(UUID organizerId, UUID eventId) {
        ownedEvent(organizerId, eventId);
        return invitations.findByEventIdOrderByCreatedAtAsc(eventId);
    }

    // INVITE_ONLY etkinlik slug'la görünmez; davetli etkinliği, bilet seçeneklerini ve formu buradan görür.
    @Transactional(readOnly = true)
    public InvitationPreview preview(String rawToken) {
        Invitation invitation = findByToken(rawToken);
        EventSummary event = eventLookup.find(invitation.getEventId())
            .orElseThrow(RegistrationTargetNotFoundException::new);
        return new InvitationPreview(invitation, event, eventLookup.findTicketOffers(event.id()),
            eventForms.findCurrent(event.id()).orElse(null));
    }

    @Transactional
    public Registration acceptAsMember(UUID userId, String rawToken, UUID ticketTypeId, Map<String, Object> answers) {
        Invitation invitation = consume(rawToken);
        return registrationService.registerMember(userId, invitation.getEventId(), ticketTypeId,
            ReservationContext.invitedMember(), answers);
    }

    @Transactional
    public Registration acceptAsGuest(String rawToken, UUID ticketTypeId, String name, Map<String, Object> answers) {
        Invitation invitation = consume(rawToken);
        return registrationService.registerInvitedGuest(invitation.getEventId(), ticketTypeId,
            invitation.getEmail(), name, answers);
    }

    // form: etkinliğin formu yoksa null.
    public record InvitationPreview(Invitation invitation, EventSummary event, List<TicketOffer> tickets,
                                    EventForm form) {
    }

    // Kayıttan önce: sonraki adımlar (dolu, zaten kayıtlı, geçersiz cevap...) hata verirse transaction daveti de geri alır.
    private Invitation consume(String rawToken) {
        Invitation invitation = findByToken(rawToken);
        invitation.accept(Instant.now());
        try {
            invitations.flush();
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new InvitationConflictException();
        }
        return invitation;
    }

    private Invitation findByToken(String rawToken) {
        return invitations.findByTokenHash(SecureTokens.hash(rawToken))
            .orElseThrow(InvalidInvitationTokenException::new);
    }

    private EventSummary ownedEvent(UUID organizerId, UUID eventId) {
        EventSummary event = eventLookup.find(eventId).orElseThrow(RegistrationTargetNotFoundException::new);
        if (!event.isOrganizedBy(organizerId)) {
            throw new EventOrganizerOnlyException();
        }
        return event;
    }
}
