package com.venyu.identity.internal.web;

import com.jayway.jsonpath.JsonPath;
import com.venyu.TestcontainersConfiguration;
import com.venyu.identity.EmailVerificationRequested;
import com.venyu.identity.PhoneVerificationRequested;
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
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ApplicationModuleTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
@RecordApplicationEvents
class UserJourneyTests {

    private static final String EMAIL = "yolculuk@example.com";
    private static final String PASSWORD = "guclu-sifre-123";
    private static final String PHONE = "+905559876543";

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationEvents events;

    @Test
    void registerVerifyEmailLoginVerifyPhone() throws Exception {
        // 1) Kayıt
        post("/auth/register", null, """
                {"email": "%s", "password": "%s", "displayName": "Yolcu"}
                """.formatted(EMAIL, PASSWORD))
                .andExpect(status().isCreated());

        // 2) Login: profil var, henüz hiçbir şey doğrulanmamış
        String accessToken = JsonPath.read(post("/auth/login", null, """
                {"email": "%s", "password": "%s"}
                """.formatted(EMAIL, PASSWORD))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.accessToken");

        me(accessToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.displayName").value("Yolcu"))
                .andExpect(jsonPath("$.roles[0]").value("PARTICIPANT"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.phoneVerified").value(false));

        // 3) E-posta doğrulama: aynı token'la /me anında güncel durumu gösterir
        String emailToken = events.stream(EmailVerificationRequested.class).findFirst().orElseThrow().token();
        post("/auth/verify-email", null, """
                {"token": "%s"}
                """.formatted(emailToken))
                .andExpect(status().isNoContent());

        me(accessToken).andExpect(jsonPath("$.emailVerified").value(true));

        // 4) Telefon doğrulama
        post("/me/phone/verification", accessToken, """
                {"phone": "%s"}
                """.formatted(PHONE))
                .andExpect(status().isAccepted());
        String code = events.stream(PhoneVerificationRequested.class).findFirst().orElseThrow().code();
        post("/me/phone/verification/confirm", accessToken, """
                {"code": "%s"}
                """.formatted(code))
                .andExpect(status().isNoContent());

        me(accessToken)
                .andExpect(jsonPath("$.phone").value(PHONE))
                .andExpect(jsonPath("$.phoneVerified").value(true));
    }

    private ResultActions post(String path, String accessToken, String body) throws Exception {
        var request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (accessToken != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        return mvc.perform(request);
    }

    private ResultActions me(String accessToken) throws Exception {
        return mvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
    }
}