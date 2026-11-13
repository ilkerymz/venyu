package com.venyu.identity.internal;

import com.venyu.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ApplicationModuleTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class RegistrationTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Test
    void registersUserAndStoresOnlyPasswordHash() throws Exception {
        register("Zeynep@Example.com", "guclu-sifre-123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("zeynep@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        User user = users.findByEmail("zeynep@example.com").orElseThrow();
        assertThat(user.getPasswordHash())
                .startsWith("{bcrypt}")
                .doesNotContain("guclu-sifre-123");
    }

    @Test
    void duplicateEmailReturnsConflictAsProblemDetail() throws Exception {
        register("can@example.com", "guclu-sifre-123").andExpect(status().isCreated());

        register("CAN@example.com", "baska-sifre-456")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void invalidRequestReturnsBadRequest() throws Exception {
        register("gecersiz-eposta", "123").andExpect(status().isBadRequest());
    }

    private ResultActions register(String email, String password) throws Exception {
        return mvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s", "displayName": "Test"}
                        """.formatted(email, password)));
    }
}
