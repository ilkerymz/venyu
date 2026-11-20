package com.venyu.identity.internal.web;

import com.jayway.jsonpath.JsonPath;
import com.venyu.TestcontainersConfiguration;
import com.venyu.identity.PasswordResetRequested;
import com.venyu.identity.internal.repository.UserRepository;
import com.venyu.identity.internal.service.UserRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ApplicationModuleTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
@RecordApplicationEvents
class PasswordResetTests {

    private static final String EMAIL = "mert@example.com";
    private static final String OLD_PASSWORD = "eski-sifre-123";
    private static final String NEW_PASSWORD = "yeni-sifre-456";

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationEvents events;

    @Autowired
    UserRegistrationService registrationService;

    @Autowired
    UserRepository users;

    @BeforeEach
    void registerUser() {
        registrationService.register(EMAIL, OLD_PASSWORD, "Mert");
    }

    @Test
    void requestForUnknownEmailReturnsAcceptedWithoutEvent() throws Exception {
        requestReset("yok@example.com").andExpect(status().isAccepted());

        assertThat(resetEvents()).isEmpty();
    }

    @Test
    void resetChangesPasswordAndVerifiesEmail() throws Exception {
        requestReset("Mert@Example.com").andExpect(status().isAccepted());

        confirm(lastToken(), NEW_PASSWORD).andExpect(status().isNoContent());

        login(OLD_PASSWORD).andExpect(status().isUnauthorized());
        login(NEW_PASSWORD).andExpect(status().isOk());
        assertThat(users.findByEmail(EMAIL).orElseThrow().isEmailVerified()).isTrue();
    }

    @Test
    void resetRevokesExistingRefreshTokens() throws Exception {
        String refreshToken = JsonPath.read(
                login(OLD_PASSWORD).andReturn().getResponse().getContentAsString(), "$.refreshToken");
        requestReset(EMAIL);

        confirm(lastToken(), NEW_PASSWORD).andExpect(status().isNoContent());

        mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenCanBeUsedOnlyOnce() throws Exception {
        requestReset(EMAIL);
        String token = lastToken();

        confirm(token, NEW_PASSWORD).andExpect(status().isNoContent());
        confirm(token, "baska-sifre-789").andExpect(status().isBadRequest());
    }

    @Test
    void newRequestInvalidatesPreviousToken() throws Exception {
        requestReset(EMAIL);
        String firstToken = lastToken();
        requestReset(EMAIL);
        String secondToken = lastToken();

        confirm(firstToken, NEW_PASSWORD).andExpect(status().isBadRequest());
        confirm(secondToken, NEW_PASSWORD).andExpect(status().isNoContent());
    }

    @Test
    void tooLongPasswordDoesNotConsumeToken() throws Exception {
        requestReset(EMAIL);
        String token = lastToken();
        String seventyTwoCharsButTooManyBytes = "ş".repeat(40);

        confirm(token, seventyTwoCharsButTooManyBytes).andExpect(status().isBadRequest());
        confirm(token, NEW_PASSWORD).andExpect(status().isNoContent());
    }

    private ResultActions requestReset(String email) throws Exception {
        return mvc.perform(post("/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s"}
                        """.formatted(email)));
    }

    private ResultActions confirm(String token, String newPassword) throws Exception {
        return mvc.perform(post("/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token": "%s", "newPassword": "%s"}
                        """.formatted(token, newPassword)));
    }

    private ResultActions login(String password) throws Exception {
        return mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(EMAIL, password)));
    }

    private List<PasswordResetRequested> resetEvents() {
        return events.stream(PasswordResetRequested.class).toList();
    }

    private String lastToken() {
        return resetEvents().getLast().token();
    }
}