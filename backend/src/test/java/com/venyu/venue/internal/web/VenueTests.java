package com.venyu.venue.internal.web;

import com.jayway.jsonpath.JsonPath;
import com.venyu.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Güvenlik (SecurityConfig, method security) identity modülünde: venue'nun web katmanı ona dayanıyor (ADR-006).
@ApplicationModuleTest(extraIncludes = "identity")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class VenueTests {

    private static final String VENUE_JSON = """
            {"name": "%s", "description": "80 kişilik salon", "address": "Moda Cd. 1",
             "district": "Kadıköy", "city": "İstanbul", "capacity": %d}
            """;

    @Autowired
    MockMvc mvc;

    UUID owner = UUID.randomUUID();
    UUID otherOwner = UUID.randomUUID();

    @Test
    void venueOwnerCanCreateVenue() throws Exception {
        create(venueOwner(owner), "Kadıköy Sahne", 80)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.ownerId").value(owner.toString()))
                .andExpect(jsonPath("$.name").value("Kadıköy Sahne"));
    }

    @Test
    void onlyVenueOwnersCanCreate() throws Exception {
        create(participant(UUID.randomUUID()), "Sahte Mekan", 80).andExpect(status().isForbidden());
        mvc.perform(post("/venues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENUE_JSON.formatted("Anonim", 80)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anyoneCanListAndViewVenues() throws Exception {
        String id = createAndGetId(owner, "Kadıköy Sahne");

        mvc.perform(get("/venues/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kadıköy Sahne"));
        mvc.perform(get("/venues").param("district", "Kadıköy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/venues").param("district", "Beşiktaş"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void ownerCanUpdateOwnVenue() throws Exception {
        String id = createAndGetId(owner, "Eski Ad");

        update(venueOwner(owner), id, "Yeni Ad")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Yeni Ad"));
    }

    @Test
    void ownerCannotUpdateSomeoneElsesVenue() throws Exception {
        String id = createAndGetId(owner, "Benim Mekanım");

        // IDOR regresyon testi: rol doğru (VENUE_OWNER) ama mekan onun değil.
        update(venueOwner(otherOwner), id, "Ele Geçirildi").andExpect(status().isForbidden());

        mvc.perform(get("/venues/{id}", id)).andExpect(jsonPath("$.name").value("Benim Mekanım"));
    }

    @Test
    void invalidInputAndUnknownVenue() throws Exception {
        create(venueOwner(owner), "Kapasitesiz", 0).andExpect(status().isBadRequest());
        mvc.perform(get("/venues/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    private ResultActions create(RequestPostProcessor user, String name, int capacity) throws Exception {
        return mvc.perform(post("/venues").with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VENUE_JSON.formatted(name, capacity)));
    }

    private ResultActions update(RequestPostProcessor user, String id, String name) throws Exception {
        return mvc.perform(put("/venues/{id}", id).with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VENUE_JSON.formatted(name, 80)));
    }

    private String createAndGetId(UUID ownerId, String name) throws Exception {
        String body = create(venueOwner(ownerId), name, 80).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private static RequestPostProcessor venueOwner(UUID userId) {
        return jwt().jwt(j -> j.subject(userId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_VENUE_OWNER"));
    }

    private static RequestPostProcessor participant(UUID userId) {
        return jwt().jwt(j -> j.subject(userId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_PARTICIPANT"));
    }
}