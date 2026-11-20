package com.venyu.identity.internal.repository;

import com.venyu.TestcontainersConfiguration;
import com.venyu.identity.internal.domain.RefreshToken;
import com.venyu.identity.internal.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
@Transactional
class RefreshTokenRepositoryTests {

    @Autowired
    UserRepository users;

    @Autowired
    RefreshTokenRepository refreshTokens;

    User user;

    @BeforeEach
    void createUser() {
        user = users.saveAndFlush(User.register("deniz@example.com", "hash", "Deniz"));
    }

    @Test
    void activeTokenCanBeRevokedOnlyOnce() {
        saveToken("hash-1", Instant.now().plus(Duration.ofDays(1)));

        assertThat(refreshTokens.revokeIfActive("hash-1", Instant.now())).isEqualTo(1);
        assertThat(refreshTokens.revokeIfActive("hash-1", Instant.now())).isZero();
    }

    @Test
    void expiredTokenIsNotRevoked() {
        saveToken("hash-expired", Instant.now().minus(Duration.ofMinutes(1)));

        assertThat(refreshTokens.revokeIfActive("hash-expired", Instant.now())).isZero();
    }

    @Test
    void revokeAllForUserRevokesOnlyActiveTokens() {
        saveToken("hash-a", Instant.now().plus(Duration.ofDays(1)));
        saveToken("hash-b", Instant.now().plus(Duration.ofDays(1)));
        refreshTokens.revokeIfActive("hash-a", Instant.now());

        assertThat(refreshTokens.revokeAllForUser(user.getId(), Instant.now())).isEqualTo(1);
    }

    private void saveToken(String hash, Instant expiresAt) {
        refreshTokens.saveAndFlush(RefreshToken.issue(user.getId(), hash, expiresAt));
    }
}