package com.venyu.registration.internal.service;

import com.venyu.event.EventLookup;
import com.venyu.event.EventSummary;
import com.venyu.event.ReservationContext;
import com.venyu.event.TicketInventory;
import com.venyu.event.TicketReservation;
import com.venyu.registration.GuestConfirmationRequested;
import com.venyu.registration.internal.domain.Registration;
import com.venyu.registration.internal.domain.RegistrationStatus;
import com.venyu.registration.internal.exception.AlreadyRegisteredException;
import com.venyu.registration.internal.exception.InvalidConfirmationTokenException;
import com.venyu.registration.internal.exception.RegistrationConflictException;
import com.venyu.registration.internal.exception.RegistrationNotAvailableException;
import com.venyu.registration.internal.exception.RegistrationNotFoundException;
import com.venyu.registration.internal.exception.RegistrationTargetNotFoundException;
import com.venyu.registration.internal.exception.SoldOutException;
import com.venyu.registration.internal.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    // Misafirin e-postasını onaylaması için süre; dolunca koltuk bırakılır (ExpiredRegistrationJob).
    static final Duration GUEST_CONFIRMATION_WINDOW = Duration.ofMinutes(30);

    private final RegistrationRepository registrations;
    private final TicketInventory inventory;
    private final EventLookup eventLookup;
    private final ApplicationEventPublisher events;

    @Transactional
    public Registration register(UUID userId, UUID eventId, UUID ticketTypeId) {
        return registerMember(userId, eventId, ticketTypeId, ReservationContext.member());
    }

    // Davetli üye için InvitationService de çağırır (context: invitedMember).
    @Transactional
    public Registration registerMember(UUID userId, UUID eventId, UUID ticketTypeId, ReservationContext context) {
        // Ucuz ön kontrol: zaten kayıtlıysa koltuk kilidine hiç girme. Son garanti partial unique index.
        if (registrations.existsByEventIdAndUserIdAndStatusIn(eventId, userId, RegistrationStatus.ACTIVE)) {
            throw new AlreadyRegisteredException();
        }
        reserveSeat(eventId, ticketTypeId, context);
        return saveNew(Registration.confirmed(eventId, ticketTypeId, userId));
    }

    @Transactional
    public Registration registerGuest(UUID eventId, UUID ticketTypeId, String email, String name) {
        String normalizedEmail = Emails.normalize(email);
        requireNoActiveGuestRegistration(eventId, normalizedEmail);
        reserveSeat(eventId, ticketTypeId, ReservationContext.guest());
        // reserve başarılıysa etkinlik var; lookup sadece bildirim metni (başlık) için.
        EventSummary event = eventLookup.find(eventId).orElseThrow(RegistrationTargetNotFoundException::new);

        String token = SecureTokens.generate();
        Instant expiresAt = Instant.now().plus(GUEST_CONFIRMATION_WINDOW);
        Registration registration = saveNew(Registration.reservedGuest(eventId, ticketTypeId, normalizedEmail,
            name.trim(), SecureTokens.hash(token), expiresAt));
        // Outbox: transaction commit olursa e-posta gider, geri alınırsa gitmez.
        events.publishEvent(new GuestConfirmationRequested(registration.getId(), normalizedEmail,
            registration.getGuestName(), event.title(), token, expiresAt));
        return registration;
    }

    // Davetli misafir (InvitationService): e-posta davetle kanıtlandı, doğrudan CONFIRMED.
    @Transactional
    public Registration registerInvitedGuest(UUID eventId, UUID ticketTypeId, String email, String name) {
        requireNoActiveGuestRegistration(eventId, email);
        reserveSeat(eventId, ticketTypeId, ReservationContext.invitedGuest());
        return saveNew(Registration.confirmedGuest(eventId, ticketTypeId, email, name.trim()));
    }

    @Transactional
    public Registration confirmGuest(String rawToken) {
        Registration registration = registrations.findByConfirmationTokenHash(SecureTokens.hash(rawToken))
            .orElseThrow(InvalidConfirmationTokenException::new);
        registration.confirm(Instant.now());
        flush(); // Expiry job ile yarış: aynı satıra ikisi birden dokunursa biri optimistic hata alır.
        return registration;
    }

    @Transactional
    public Registration cancel(UUID userId, UUID registrationId) {
        Registration registration = registrations.findByIdAndUserId(registrationId, userId)
            .orElseThrow(RegistrationNotFoundException::new);
        registration.cancel(Instant.now());
        // release'ten önce: release içindeki bulk update persistence context'i temizliyor.
        flush();
        inventory.release(registration.getTicketTypeId());
        return registration;
    }

    // ExpiredRegistrationJob çağırır. Optimistic hata tüm turu geri alır; job bir sonraki turda tekrar dener.
    @Transactional
    public int expireReservations() {
        Instant now = Instant.now();
        List<Registration> expired = registrations
            .findTop100ByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(RegistrationStatus.RESERVED, now);
        expired.forEach(registration -> registration.expire(now));
        registrations.flush(); // release'ten önce (cancel'daki ile aynı neden)
        expired.forEach(registration -> inventory.release(registration.getTicketTypeId()));
        return expired.size();
    }

    @Transactional(readOnly = true)
    public List<Registration> listMine(UUID userId) {
        return registrations.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private void requireNoActiveGuestRegistration(UUID eventId, String normalizedEmail) {
        if (registrations.existsByEventIdAndGuestEmailAndStatusIn(eventId, normalizedEmail,
            RegistrationStatus.ACTIVE)) {
            throw new AlreadyRegisteredException();
        }
    }

    private void reserveSeat(UUID eventId, UUID ticketTypeId, ReservationContext context) {
        switch (inventory.reserve(eventId, ticketTypeId, context)) {
            case TicketReservation.Reserved reserved -> {
            }
            case TicketReservation.SoldOut soldOut -> throw new SoldOutException();
            case TicketReservation.NotAvailable notAvailable ->
                throw new RegistrationNotAvailableException(notAvailable.reason());
            case TicketReservation.NotFound notFound -> throw new RegistrationTargetNotFoundException();
        }
    }

    private Registration saveNew(Registration registration) {
        try {
            return registrations.saveAndFlush(registration);
        } catch (DataIntegrityViolationException e) {
            // Aynı kişinin eşzamanlı ikinci isteği (partial unique index). Exception transaction'ı geri alır:
            // reserveSeat'in artırdığı koltuk da geri alınır (aynı transaction).
            throw new AlreadyRegisteredException();
        }
    }

    private void flush() {
        try {
            registrations.flush();
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RegistrationConflictException();
        }
    }
}
