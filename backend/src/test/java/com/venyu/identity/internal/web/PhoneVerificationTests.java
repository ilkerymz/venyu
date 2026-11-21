package com.venyu.identity.internal.web;

import com.jayway.jsonpath.JsonPath;
import com.venyu.TestcontainersConfiguration;
import com.venyu.identity.PhoneVerificationRequested;
import com.venyu.identity.internal.domain.User;
import com.venyu.identity.internal.exception.TooManyOtpRequestsException;
import com.venyu.identity.internal.repository.UserRepository;
import com.venyu.identity.internal.service.PhoneVerificationService;
import com.venyu.identity.internal.service.UserRegistrationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Bilinçli olarak @Transactional değil: deneme sayacının commit'ini ve eşzamanlı rate limit'i gerçek haliyle test ediyoruz.
@ApplicationModuleTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@RecordApplicationEvents
class PhoneVerificationTests {

    private static final String PASSWORD = "guclu-sifre-123";
    private static final String PHONE = "+905551234567";

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationEvents events;

    @Autowired
    UserRegistrationService registrationService;

    @Autowired
    PhoneVerificationService phoneVerificationService;

    @Autowired
    UserRepository users;

    User user;
    String accessToken;

    @BeforeEach
    void registerAndLogin() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        user = registrationService.register(email, PASSWORD, "Test");
        String body = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        accessToken = JsonPath.read(body, "$.accessToken");
    }

    @AfterEach
    void cleanUp() {
        users.deleteAll();
    }

    @Test
    void validCodeVerifiesPhone() throws Exception {
        requestCode(PHONE).andExpect(status().isAccepted());

        confirm(lastCode()).andExpect(status().isNoContent());

        User verified = users.findById(user.getId()).orElseThrow();
        assertThat(verified.isPhoneVerified()).isTrue();
        assertThat(verified.getPhone()).isEqualTo(PHONE);
    }

    @Test
    void attemptsAreLimitedEvenThoughWrongCodesThrow() throws Exception {
        requestCode(PHONE);
        String code = lastCode();
        String wrongCode = code.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            confirm(wrongCode).andExpect(status().isBadRequest());
        }

        // 5 hak bitti: doğru kod bile reddedilir. Sayaç rollback olsaydı burası 204 dönerdi.
        confirm(code).andExpect(status().isBadRequest());
    }

    @Test
    void fourthRequestWithinWindowIsRateLimited() throws Exception {
        for (int i = 0; i < 3; i++) {
            requestCode(PHONE).andExpect(status().isAccepted());
        }
        requestCode(PHONE).andExpect(status().isTooManyRequests());
    }

    @Test
    void newRequestInvalidatesPreviousCode() throws Exception {
        requestCode(PHONE);
        String firstCode = lastCode();
        requestCode(PHONE);
        String secondCode = lastCode();

        if (!firstCode.equals(secondCode)) {
            confirm(firstCode).andExpect(status().isBadRequest());
        }
        confirm(secondCode).andExpect(status().isNoContent());
    }

    @Test
    void phoneVerifiedByAnotherUserReturnsConflict() throws Exception {
        User other = registrationService.register("other-" + UUID.randomUUID() + "@example.com", PASSWORD, "Diğer");
        other.verifyPhone(PHONE, Instant.now());
        users.save(other);

        requestCode(PHONE).andExpect(status().isConflict());
    }

    @Test
    void requiresAuthenticationAndValidInput() throws Exception {
        mvc.perform(post("/me/phone/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone": "%s"}
                                """.formatted(PHONE)))
                .andExpect(status().isUnauthorized());

        requestCode("05551234567").andExpect(status().isBadRequest());
        confirm("12ab").andExpect(status().isBadRequest());
    }

    @Test
    void concurrentRequestsCannotExceedRateLimit() throws Exception {
        int threads = 6;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            results.add(executor.submit(() -> {
                start.await();
                try {
                    phoneVerificationService.requestCode(user.getId(), PHONE);
                    return true;
                } catch (TooManyOtpRequestsException e) {
                    return false;
                }
            }));
        }
        start.countDown();

        int accepted = 0;
        for (Future<Boolean> result : results) {
            if (result.get(10, TimeUnit.SECONDS)) {
                accepted++;
            }
        }
        executor.shutdown();

        assertThat(accepted).isEqualTo(3);
    }

    private ResultActions requestCode(String phone) throws Exception {
        return mvc.perform(post("/me/phone/verification")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"phone": "%s"}
                        """.formatted(phone)));
    }

    private ResultActions confirm(String code) throws Exception {
        return mvc.perform(post("/me/phone/verification/confirm")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code": "%s"}
                        """.formatted(code)));
    }

    private String lastCode() {
        return events.stream(PhoneVerificationRequested.class).toList().getLast().code();
    }
}