package com.venyu;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Etkinlik kurulumu, event modülünün public HTTP API'si üzerinden (event'e bağımlı modüllerin testleri için).
 * Not: RegistrationTests ve GuestAndInvitationTests'teki eşdeğer yardımcılar henüz buraya taşınmadı.
 */
public final class EventFixtures {

    private final MockMvc mvc;
    private final UUID organizer;
    private final Instant start = Instant.now().plus(Duration.ofDays(7)).truncatedTo(ChronoUnit.HOURS);

    public EventFixtures(MockMvc mvc, UUID organizer) {
        this.mvc = mvc;
        this.organizer = organizer;
    }

    public static RequestPostProcessor user(UUID userId, String role) {
        return jwt().jwt(j -> j.subject(userId.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    // Taslak, adresli etkinlik; id döner. Otomatik "Genel Katılım" bileti (ücretsiz, sınırsız) oluşur.
    public String draftEvent(String visibility, boolean requiresAccount) throws Exception {
        return JsonPath.read(mvc.perform(post("/events").with(user(organizer, "ORGANIZER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Test", "visibility": "%s", "requiresAccount": %s, "customAddress": "Moda",
                     "startsAt": "%s", "endsAt": "%s"}
                    """.formatted(visibility, requiresAccount, start.atOffset(ZoneOffset.UTC),
                    start.plus(Duration.ofHours(2)).atOffset(ZoneOffset.UTC))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString(), "$.id");
    }

    // Ücretsiz bilet türü; id döner.
    public String ticket(String eventId, String name, int capacity) throws Exception {
        return JsonPath.read(mvc.perform(post("/events/{id}/ticket-types", eventId)
                .with(user(organizer, "ORGANIZER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "%s", "price": 0, "capacity": %d}
                    """.formatted(name, capacity)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString(), "$.id");
    }

    public void publish(String eventId) throws Exception {
        mvc.perform(post("/events/{id}/publish", eventId).with(user(organizer, "ORGANIZER")))
            .andExpect(status().isOk());
    }

    public int sold(String eventId, String ticketTypeId) throws Exception {
        String body = mvc.perform(get("/events/{id}/ticket-types", eventId).with(user(organizer, "ORGANIZER")))
            .andReturn().getResponse().getContentAsString();
        List<Integer> sold = JsonPath.read(body, "$[?(@.id == '" + ticketTypeId + "')].sold");
        return sold.getFirst();
    }
}
