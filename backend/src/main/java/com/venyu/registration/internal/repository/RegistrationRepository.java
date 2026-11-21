package com.venyu.registration.internal.repository;

import com.venyu.registration.internal.domain.Registration;
import com.venyu.registration.internal.domain.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

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

    Optional<Registration> findByEventIdAndTicketCode(UUID eventId, String ticketCode);

    // Katılımcı listesi: süresi dolmuş (hiç onaylanmamış) misafir kayıtları gürültü.
    List<Registration> findByEventIdAndStatusNotOrderByCreatedAtAsc(UUID eventId, RegistrationStatus status);

    // Check-in: tek koşullu UPDATE. Aynı bilet iki kapıda aynı anda okutulursa sadece biri 1 satır günceller.
    // event_id koşulu: başka etkinliğin bileti bu etkinlikte geçmez. version + 1: @Version'lı okumalar bayat kalmasın.
    // Native: checked_in_* alanları entity'de yazılamaz (insertable/updatable = false), sadece bu sorgu yazar.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        UPDATE registrations
        SET status = 'CHECKED_IN', checked_in_at = :now, checked_in_by = :checkedInBy,
            updated_at = :now, version = version + 1
        WHERE event_id = :eventId AND ticket_code = :ticketCode AND status = 'CONFIRMED'
        """, nativeQuery = true)
    int checkIn(UUID eventId, String ticketCode, UUID checkedInBy, Instant now);

    // Expiry job: tur başına sınırlı sayıda (registrations_reserved_expiry_idx).
    List<Registration> findTop100ByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(RegistrationStatus status,
                                                                               Instant now);

    List<Registration> findByEventIdAndStatusIn(UUID eventId, Collection<RegistrationStatus> statuses);
}
