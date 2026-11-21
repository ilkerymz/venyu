package com.venyu.registration.internal.domain;

import com.venyu.registration.internal.exception.RegistrationStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "registrations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false, updatable = false)
    private UUID ticketTypeId;

    @Column(nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status;

    @Version
    private long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant cancelledAt;

    // F2: ücretsiz bilet, kayıt anında onaylı. Ücretli akış (RESERVED → PENDING_THRESHOLD) F3'te.
    public static Registration confirmed(UUID eventId, UUID ticketTypeId, UUID userId) {
        Registration registration = new Registration();
        registration.eventId = eventId;
        registration.ticketTypeId = ticketTypeId;
        registration.userId = userId;
        registration.status = RegistrationStatus.CONFIRMED;
        registration.createdAt = Instant.now();
        registration.updatedAt = registration.createdAt;
        return registration;
    }

    public void cancel(Instant now) {
        if (status != RegistrationStatus.CONFIRMED) {
            throw new RegistrationStateException("Sadece onaylı kayıtlar iptal edilebilir.");
        }
        this.status = RegistrationStatus.CANCELLED;
        this.cancelledAt = now;
        this.updatedAt = now;
    }
}
