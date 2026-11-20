package com.venyu.identity.internal.repository;

import com.venyu.identity.internal.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update PasswordResetToken t set t.consumedAt = :now
            where t.tokenHash = :tokenHash and t.consumedAt is null and t.expiresAt > :now
            """)
    int consumeIfValid(String tokenHash, Instant now);

    // Yeni istek gelince eski link'ler geçersiz olsun: kullanılmamış token'lar tüketilmiş sayılır.
    @Modifying
    @Query("update PasswordResetToken t set t.consumedAt = :now where t.userId = :userId and t.consumedAt is null")
    int invalidateActiveForUser(UUID userId, Instant now);
}