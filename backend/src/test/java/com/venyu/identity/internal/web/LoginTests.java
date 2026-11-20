package com.venyu.identity.internal.web;

import com.jayway.jsonpath.JsonPath;
import com.venyu.TestcontainersConfiguration;
import com.venyu.identity.internal.domain.RefreshToken;
import com.venyu.identity.internal.domain.User;
import com.venyu.identity.internal.repository.RefreshTokenRepository;
import com.venyu.identity.internal.service.UserRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ApplicationModuleTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class LoginTests {

    private static final String EMAIL = "elif@example.com";
    private static final String PASSWORD = "guclu-sifre-123";

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRegistrationService registrationService;

    @Autowired
    RefreshTokenRepository refreshTokens;

    User user;

    @BeforeEach
    void registerUser() {
        user = registrationService.register(EMAIL, PASSWORD, "Elif");
    }

    @Test
    void loginWithValidCredentialsReturnsTokens() throws Exception {
        login("Elif@Example.com", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void wrongPasswordAndUnknownEmailReturnSameUnauthorizedResponse() throws Exception {
        String wrongPassword = login(EMAIL, "yanlis-sifre-999")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String unknownEmail = login("yok@example.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(wrongPassword).isEqualTo(unknownEmail);
    }

    @Test
    void meWithoutTokenReturnsUnauthorized() throws Exception {
        mvc.perform(get("/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void meWithAccessTokenReturnsUserFromToken() throws Exception {
        String accessToken = JsonPath.read(loginAndGetBody(), "$.accessToken");

        mvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.roles[0]").value("PARTICIPANT"));
    }

    @Test
    void refreshTokenIsStoredOnlyAsHash() throws Exception {
        String refreshToken = JsonPath.read(loginAndGetBody(), "$.refreshToken");

        List<RefreshToken> stored = refreshTokens.findAll().stream()
                .filter(t -> t.getUserId().equals(user.getId()))
                .toList();

        assertThat(stored).hasSize(1);
        assertThat(stored.getFirst().getTokenHash())
                .hasSize(64)
                .isNotEqualTo(refreshToken);
    }

    @Test
    void loginIgnoresInvalidBearerHeader() throws Exception {
        mvc.perform(post("/auth/login")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-garbage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointStillRejectsInvalidBearerToken() throws Exception {
        mvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-garbage"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password)));
    }

    private String loginAndGetBody() throws Exception {
        return login(EMAIL, PASSWORD).andReturn().getResponse().getContentAsString();
    }
}