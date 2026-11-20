package com.venyu.identity.internal.repository;

import com.venyu.TestcontainersConfiguration;
import com.venyu.identity.internal.domain.EmailVerificationToken;
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
class EmailVerificationTokenRepositoryTests {

    @Autowired
    UserRepository users;

    @Autowired
    EmailVerificationTokenRepository tokens;

    User user;

    @BeforeEach
    void createUser() {
        user = users.saveAndFlush(User.register("ece@example.com", "hash", "Ece"));
    }

    @Test
    void validTokenCanBeConsumedOnlyOnce() {
        save("hash-1", Instant.now().plus(Duration.ofHours(1)));

        assertThat(tokens.consumeIfValid("hash-1", Instant.now())).isEqualTo(1);
        assertThat(tokens.consumeIfValid("hash-1", Instant.now())).isZero();
    }

    @Test
    void expiredTokenCannotBeConsumed() {
        save("hash-expired", Instant.now().minus(Duration.ofMinutes(1)));

        assertThat(tokens.consumeIfValid("hash-expired", Instant.now())).isZero();
    }

    private void save(String hash, Instant expiresAt) {
        tokens.saveAndFlush(EmailVerificationToken.issue(user.getId(), hash, expiresAt));
    }
}