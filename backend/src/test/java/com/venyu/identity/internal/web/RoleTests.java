package com.venyu.identity.internal.web;

import com.jayway.jsonpath.JsonPath;
import com.venyu.TestcontainersConfiguration;
import com.venyu.identity.internal.domain.User;
import com.venyu.identity.internal.service.RoleService;
import com.venyu.identity.internal.service.UserRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ApplicationModuleTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class RoleTests {

    private static final String EMAIL = "rol@example.com";
    private static final String PASSWORD = "guclu-sifre-123";

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRegistrationService registrationService;

    @Autowired
    RoleService roleService;

    @Autowired
    JwtDecoder jwtDecoder;

    User user;
    String accessToken;
    String refreshToken;

    @BeforeEach
    void registerAndLogin() throws Exception {
        user = registrationService.register(EMAIL, PASSWORD, "Rol");
        String body = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(EMAIL, PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        accessToken = JsonPath.read(body, "$.accessToken");
        refreshToken = JsonPath.read(body, "$.refreshToken");
    }

    @Test
    void userCanBecomeOrganizerAndRefreshedTokenCarriesRole() throws Exception {
        mvc.perform(post("/me/roles/organizer").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        // Eski token hâlâ eski rolleri taşır (ADR-003: 15 dk gecikme)...
        assertThat(rolesIn(accessToken)).containsExactly("PARTICIPANT");

        // ...refresh sonrası yeni token yeni rolü taşır.
        String refreshed = JsonPath.read(mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
        assertThat(rolesIn(refreshed)).containsExactlyInAnyOrder("PARTICIPANT", "ORGANIZER");
    }

    @Test
    void becomingOrganizerTwiceIsIdempotent() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/me/roles/organizer").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isNoContent());
        }

        mvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(jsonPath("$.roles.length()").value(2))
                .andExpect(jsonPath("$.roles[0]").value("ORGANIZER"))
                .andExpect(jsonPath("$.roles[1]").value("PARTICIPANT"));
    }

    @Test
    void venueOwnerRoleIsGrantedOnlyThroughService() throws Exception {
        roleService.grantVenueOwner(user.getId());

        mvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(jsonPath("$.roles[1]").value("VENUE_OWNER"));
        mvc.perform(post("/me/roles/venue-owner").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    private Iterable<String> rolesIn(String token) {
        return jwtDecoder.decode(token).getClaimAsStringList("roles");
    }
}