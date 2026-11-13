package com.venyu.identity.internal;

import com.venyu.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserRepositoryTests {

    @Autowired
    UserRepository users;

    @Test
    void newUserGetsParticipantRoleAndNormalizedEmail() {
        users.saveAndFlush(User.register("  Ayse@Example.COM ", "hash", "Ayşe"));

        User found = users.findByEmail("ayse@example.com").orElseThrow();

        assertThat(found.getRoles()).containsExactly(Role.PARTICIPANT);
        assertThat(found.isEmailVerified()).isFalse();
        assertThat(found.isPhoneVerified()).isFalse();
    }

    @Test
    void sameEmailWithDifferentCaseIsRejected() {
        users.saveAndFlush(User.register("ali@example.com", "hash", "Ali"));

        assertThatThrownBy(() -> users.saveAndFlush(User.register("ALI@example.com", "hash", "Ali 2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
