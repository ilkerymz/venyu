package com.venyu.identity.internal.repository;

import com.venyu.identity.internal.domain.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    boolean existsByUserIdAndCreatedAtAfter(UUID userId, Instant after);

    // Atomik tek kullanım: aynı token'la gelen eşzamanlı isteklerden sadece biri 1 alır.
    @Modifying
    @Query("""
            update EmailVerificationToken t set t.consumedAt = :now
            where t.tokenHash = :tokenHash and t.consumedAt is null and t.expiresAt > :now
            """)
    int consumeIfValid(String tokenHash, Instant now);

    // Yeni bağlantı gönderilince eskiler geçersiz olur (sadece son e-postadaki bağlantı çalışır).
    @Modifying
    @Query("update EmailVerificationToken t set t.consumedAt = :now where t.userId = :userId and t.consumedAt is null")
    int invalidateActiveForUser(UUID userId, Instant now);
}
