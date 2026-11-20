package com.venyu.identity.internal.web;

import com.jayway.jsonpath.JsonPath;
import com.venyu.TestcontainersConfiguration;
import com.venyu.identity.internal.exception.InvalidRefreshTokenException;
import com.venyu.identity.internal.repository.UserRepository;
import com.venyu.identity.internal.service.AuthenticationService;
import com.venyu.identity.internal.service.UserRegistrationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Bilinçli olarak @Transactional değil: commit/rollback davranışını ve eşzamanlılığı gerçek haliyle test ediyoruz.
@ApplicationModuleTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RefreshTests {

    private static final String PASSWORD = "guclu-sifre-123";

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRegistrationService registrationService;

    @Autowired
    AuthenticationService authenticationService;

    @Autowired
    UserRepository users;

    String email;

    @BeforeEach
    void registerUser() {
        email = "user-" + UUID.randomUUID() + "@example.com";
        registrationService.register(email, PASSWORD, "Test");
    }

    @AfterEach
    void cleanUp() {
        users.deleteAll();
    }

    @Test
    void refreshRotatesTokens() throws Exception {
        String oldToken = loginAndGetRefreshToken();

        String body = refresh(oldToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String newToken = JsonPath.read(body, "$.refreshToken");

        assertThat(newToken).isNotEqualTo(oldToken);
        refresh(newToken).andExpect(status().isOk());
    }

    @Test
    void reusingRevokedTokenRevokesAllSessions() throws Exception {
        String first = loginAndGetRefreshToken();
        String second = JsonPath.read(
                refresh(first).andReturn().getResponse().getContentAsString(), "$.refreshToken");
        String otherSession = loginAndGetRefreshToken();

        refresh(first).andExpect(status().isUnauthorized());

        refresh(second).andExpect(status().isUnauthorized());
        refresh(otherSession).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTokenAndIsIdempotent() throws Exception {
        String token = loginAndGetRefreshToken();

        logout(token).andExpect(status().isNoContent());
        logout(token).andExpect(status().isNoContent());
        logout("bilinmeyen-token").andExpect(status().isNoContent());

        refresh(token).andExpect(status().isUnauthorized());
    }

    @Test
    void unknownTokenReturnsUnauthorizedAndBlankReturnsBadRequest() throws Exception {
        refresh("bilinmeyen-token").andExpect(status().isUnauthorized());
        refresh("").andExpect(status().isBadRequest());
    }

    @Test
    void concurrentRefreshWithSameTokenHasExactlyOneWinner() throws Exception {
        String token = loginAndGetRefreshToken();
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            results.add(executor.submit(() -> {
                start.await();
                try {
                    authenticationService.refresh(token);
                    return true;
                } catch (InvalidRefreshTokenException e) {
                    return false;
                }
            }));
        }
        start.countDown();

        int winners = 0;
        for (Future<Boolean> result : results) {
            if (result.get(10, TimeUnit.SECONDS)) {
                winners++;
            }
        }
        executor.shutdown();

        assertThat(winners).isEqualTo(1);
    }

    private String loginAndGetRefreshToken() throws Exception {
        String body = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.refreshToken");
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"refreshToken": "%s"}
                        """.formatted(refreshToken)));
    }

    private ResultActions logout(String refreshToken) throws Exception {
        return mvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"refreshToken": "%s"}
                        """.formatted(refreshToken)));
    }
}