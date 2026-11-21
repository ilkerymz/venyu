package com.venyu.registration.internal.service;

import com.venyu.event.TicketInventory;
import com.venyu.event.TicketReservation;
import com.venyu.registration.internal.domain.Registration;
import com.venyu.registration.internal.domain.RegistrationStatus;
import com.venyu.registration.internal.exception.AlreadyRegisteredException;
import com.venyu.registration.internal.exception.RegistrationConflictException;
import com.venyu.registration.internal.exception.RegistrationNotAvailableException;
import com.venyu.registration.internal.exception.RegistrationNotFoundException;
import com.venyu.registration.internal.exception.RegistrationTargetNotFoundException;
import com.venyu.registration.internal.exception.SoldOutException;
import com.venyu.registration.internal.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationRepository registrations;
    private final TicketInventory inventory;

    @Transactional
    public Registration register(UUID userId, UUID eventId, UUID ticketTypeId) {
        // Ucuz ön kontrol: zaten kayıtlıysa koltuk kilidine hiç girme. Son garanti partial unique index.
        if (registrations.existsByEventIdAndUserIdAndStatusIn(eventId, userId, RegistrationStatus.ACTIVE)) {
            throw new AlreadyRegisteredException();
        }
        return switch (inventory.reserve(eventId, ticketTypeId)) {
            case TicketReservation.Reserved reserved -> {
                try {
                    yield registrations.saveAndFlush(Registration.confirmed(eventId, ticketTypeId, userId));
                } catch (DataIntegrityViolationException e) {
                    // Aynı kullanıcının eşzamanlı ikinci isteği. Exception transaction'ı geri alır:
                    // reserveSeat'in artırdığı koltuk da geri alınır (aynı transaction).
                    throw new AlreadyRegisteredException();
                }
            }
            case TicketReservation.SoldOut soldOut -> throw new SoldOutException();
            case TicketReservation.NotAvailable notAvailable ->
                throw new RegistrationNotAvailableException(notAvailable.reason());
            case TicketReservation.NotFound notFound -> throw new RegistrationTargetNotFoundException();
        };
    }

    @Transactional
    public Registration cancel(UUID userId, UUID registrationId) {
        Registration registration = registrations.findByIdAndUserId(registrationId, userId)
            .orElseThrow(RegistrationNotFoundException::new);
        registration.cancel(Instant.now());
        try {
            // release'ten önce: release içindeki bulk update persistence context'i temizliyor.
            registrations.flush();
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RegistrationConflictException();
        }
        inventory.release(registration.getTicketTypeId());
        return registration;
    }

    @Transactional(readOnly = true)
    public List<Registration> listMine(UUID userId) {
        return registrations.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
