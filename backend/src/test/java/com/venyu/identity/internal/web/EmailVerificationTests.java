package com.venyu.identity.internal.web;

import com.venyu.TestcontainersConfiguration;
import com.venyu.identity.EmailVerificationRequested;
import com.venyu.identity.internal.domain.User;
import com.venyu.identity.internal.repository.EmailVerificationTokenRepository;
import com.venyu.identity.internal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ApplicationModuleTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
@RecordApplicationEvents
class EmailVerificationTests {

    private static final String EMAIL = "selin@example.com";

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationEvents events;

    @Autowired
    UserRepository users;

    @Autowired
    EmailVerificationTokenRepository tokens;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void registrationPublishesEventAndStoresOnlyTokenHash() throws Exception {
        register();

        EmailVerificationRequested event = publishedEvent();
        User user = users.findByEmail(EMAIL).orElseThrow();

        assertThat(event.userId()).isEqualTo(user.getId());
        assertThat(event.displayName()).isEqualTo("Selin");
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(tokens.findAll()).singleElement()
                .satisfies(t -> assertThat(t.getTokenHash()).hasSize(64).isNotEqualTo(event.token()));
    }

    @Test
    void validTokenMarksEmailAsVerified() throws Exception {
        register();

        verify(publishedEvent().token()).andExpect(status().isNoContent());

        assertThat(users.findByEmail(EMAIL).orElseThrow().isEmailVerified()).isTrue();
    }

    @Test
    void tokenCanBeUsedOnlyOnce() throws Exception {
        register();
        String token = publishedEvent().token();

        verify(token).andExpect(status().isNoContent());
        verify(token).andExpect(status().isBadRequest());
    }

    @Test
    void unknownOrBlankTokenReturnsBadRequest() throws Exception {
        verify("bilinmeyen-token").andExpect(status().isBadRequest());
        verify("").andExpect(status().isBadRequest());
    }

    @Test
    void resendSendsNewLinkAndInvalidatesTheOldOne() throws Exception {
        register();
        String oldToken = publishedEvent().token();
        pastCooldown();

        resend().andExpect(status().isAccepted());

        assertThat(events.stream(EmailVerificationRequested.class)).hasSize(2);
        String newToken = events.stream(EmailVerificationRequested.class).toList().getLast().token();
        verify(oldToken).andExpect(status().isBadRequest());
        verify(newToken).andExpect(status().isNoContent());
    }

    @Test
    void resendWithinCooldownIsRejected() throws Exception {
        register();

        resend().andExpect(status().isTooManyRequests());
    }

    @Test
    void resendForVerifiedEmailIsRejected() throws Exception {
        register();
        verify(publishedEvent().token()).andExpect(status().isNoContent());
        pastCooldown();

        resend().andExpect(status().isConflict());
    }

    private void register() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "guclu-sifre-123", "displayName": "Selin"}
                                """.formatted(EMAIL)))
                .andExpect(status().isCreated());
    }

    private ResultActions verify(String token) throws Exception {
        return mvc.perform(post("/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token": "%s"}
                        """.formatted(token)));
    }

    private EmailVerificationRequested publishedEvent() {
        return events.stream(EmailVerificationRequested.class).findFirst().orElseThrow();
    }

    private ResultActions resend() throws Exception {
        String userId = users.findByEmail(EMAIL).orElseThrow().getId().toString();
        return mvc.perform(post("/me/email-verification").with(jwt().jwt(j -> j.subject(userId))));
    }

    // Kayıtta gönderilen ilk e-postayı bir dakikalık beklemenin dışına iter.
    // Önce flush: token JPA ile kaydedildi, INSERT henüz veritabanına gitmemiş olabilir.
    private void pastCooldown() {
        tokens.flush();
        jdbc.update("UPDATE email_verification_tokens SET created_at = created_at - interval '2 minutes'");
    }
}
