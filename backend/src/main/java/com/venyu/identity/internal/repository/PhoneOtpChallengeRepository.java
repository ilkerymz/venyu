package com.venyu.identity.internal.repository;

import com.venyu.identity.internal.domain.PhoneOtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PhoneOtpChallengeRepository extends JpaRepository<PhoneOtpChallenge, UUID> {

    // phone_otp_challenges_user_created_idx (user_id, created_at) index'ini kullanır.
    long countByUserIdAndCreatedAtAfter(UUID userId, Instant since);

    Optional<PhoneOtpChallenge> findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("update PhoneOtpChallenge c set c.consumedAt = :now where c.userId = :userId and c.consumedAt is null")
    int invalidateActiveForUser(UUID userId, Instant now);

    // Atomik deneme sayacı: hak bittiyse, süre dolduysa veya kod kullanıldıysa 0 döner.
    @Modifying
    @Query("""
            update PhoneOtpChallenge c set c.attempts = c.attempts + 1
            where c.id = :id and c.attempts < :maxAttempts and c.consumedAt is null and c.expiresAt > :now
            """)
    int registerAttempt(UUID id, int maxAttempts, Instant now);

    @Modifying
    @Query("update PhoneOtpChallenge c set c.consumedAt = :now where c.id = :id and c.consumedAt is null")
    int consume(UUID id, Instant now);
}