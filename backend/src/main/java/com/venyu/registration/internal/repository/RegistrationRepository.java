package com.venyu.registration.internal.repository;

import com.venyu.registration.internal.domain.Registration;
import com.venyu.registration.internal.domain.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {

    boolean existsByEventIdAndUserIdAndStatusIn(UUID eventId, UUID userId, Collection<RegistrationStatus> statuses);

    boolean existsByEventIdAndGuestEmailAndStatusIn(UUID eventId, String guestEmail,
                                                    Collection<RegistrationStatus> statuses);

    // Kayıt sahibiyle birlikte aranır: başkasının kaydı iptal edilemesin (IDOR).
    Optional<Registration> findByIdAndUserId(UUID id, UUID userId);

    List<Registration> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Registration> findByConfirmationTokenHash(String confirmationTokenHash);

    Optional<Registration> findByManageTokenHash(String manageTokenHash);

    // Expiry job: tur başına sınırlı sayıda (registrations_reserved_expiry_idx).
    List<Registration> findTop100ByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(RegistrationStatus status,
                                                                               Instant now);
}
