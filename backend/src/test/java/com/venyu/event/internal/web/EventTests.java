package com.venyu.event.internal.web;

import com.jayway.jsonpath.JsonPath;
import com.venyu.TestcontainersConfiguration;
import com.venyu.venue.ConfirmedVenueBooking;
import com.venyu.venue.VenueBookings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Event modülü kendi sınırında test edilir: komşu modülün public API'si (VenueBookings) mock'lanır.
// VenueBookings'in gerçek davranışı venue modülünde VenueBookingQueriesTests ile doğrulanır.
@ApplicationModuleTest(extraIncludes = "identity")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class EventTests {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    VenueBookings venueBookings;

    UUID organizer = UUID.randomUUID();
    Instant start = Instant.now().plus(Duration.ofDays(7)).truncatedTo(ChronoUnit.HOURS);

    @Test
    void organizerCreatesDraftEventAtAddress() throws Exception {
        create(organizer, addressEvent("UNLISTED"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.customAddress").value("Moda Parkı"))
            .andExpect(jsonPath("$.venueId").doesNotExist())
            .andExpect(jsonPath("$.slug").isString());
    }

    @Test
    void venueEventTakesTimeAndVenueFromConfirmedBooking() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        given(venueBookings.findConfirmed(bookingId, organizer)).willReturn(Optional.of(
            new ConfirmedVenueBooking(bookingId, venueId, "Kadıköy Sahne", 80, start, start.plusSeconds(10_800))));

        create(organizer, venueEvent(bookingId))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.venueId").value(venueId.toString()))
            .andExpect(jsonPath("$.startsAt").value(start.toString()))
            .andExpect(jsonPath("$.customAddress").doesNotExist());
    }

    @Test
    void unusableBookingIsRejected() throws Exception {
        // Mock varsayılanı Optional.empty(): yok / onaylanmamış / başkasının rezervasyonu.
        create(organizer, venueEvent(UUID.randomUUID())).andExpect(status().isBadRequest());
    }

    @Test
    void locationMustBeExactlyOneAndInTheFuture() throws Exception {
        create(organizer, """
                {"title": "İkisi birden", "visibility": "PUBLIC", "venueBookingId": "%s", "customAddress": "Moda"}
                """.formatted(UUID.randomUUID())).andExpect(status().isBadRequest());
        create(organizer, """
                {"title": "Hiçbiri", "visibility": "PUBLIC"}
                """).andExpect(status().isBadRequest());
        create(organizer, """
                {"title": "Geçmiş", "visibility": "PUBLIC", "customAddress": "Moda",
                 "startsAt": "%s", "endsAt": "%s"}
                """.formatted(offset(start.minus(Duration.ofDays(30))), offset(start.minus(Duration.ofDays(29)))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void onlyOrganizersCanCreate() throws Exception {
        mvc.perform(post("/events").with(user(UUID.randomUUID(), "PARTICIPANT"))
                .contentType(MediaType.APPLICATION_JSON).content(addressEvent("PUBLIC")))
            .andExpect(status().isForbidden());
    }

    @Test
    void draftIsHiddenUntilPublished() throws Exception {
        String body = create(organizer, addressEvent("UNLISTED")).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");
        String slug = JsonPath.read(body, "$.slug");

        bySlug(slug).andExpect(status().isNotFound());
        mvc.perform(get("/events/by-slug/{slug}", slug).with(user(organizer, "ORGANIZER")))
            .andExpect(status().isOk());

        action(organizer, id, "publish").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"));

        bySlug(slug).andExpect(status().isOk());
    }

    @Test
    void inviteOnlyEventIsHiddenFromPublicEvenWhenPublished() throws Exception {
        String body = create(organizer, addressEvent("INVITE_ONLY")).andReturn().getResponse().getContentAsString();
        action(organizer, JsonPath.read(body, "$.id"), "publish").andExpect(status().isOk());

        bySlug(JsonPath.read(body, "$.slug")).andExpect(status().isNotFound());
    }

    @Test
    void cancelledEventIsHiddenAndCannotBePublished() throws Exception {
        String body = create(organizer, addressEvent("PUBLIC")).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");
        action(organizer, id, "publish");

        action(organizer, id, "cancel").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));

        bySlug(JsonPath.read(body, "$.slug")).andExpect(status().isNotFound());
        action(organizer, id, "publish").andExpect(status().isConflict());
    }

    @Test
    void onlyTheOrganizerCanChangeTheEvent() throws Exception {
        String id = JsonPath.read(create(organizer, addressEvent("PUBLIC"))
            .andReturn().getResponse().getContentAsString(), "$.id");
        UUID other = UUID.randomUUID();

        mvc.perform(put("/events/{id}", id).with(user(other, "ORGANIZER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {"title": "Ele geçirildi", "visibility": "PUBLIC"}
                                """))
            .andExpect(status().isForbidden());
        action(other, id, "publish").andExpect(status().isForbidden());

        mvc.perform(get("/me/events").with(user(organizer, "ORGANIZER")))
            .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/me/events").with(user(other, "ORGANIZER")))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void bookingCanBeUsedForOnlyOneEvent() throws Exception {
        UUID bookingId = UUID.randomUUID();
        given(venueBookings.findConfirmed(bookingId, organizer)).willReturn(Optional.of(
            new ConfirmedVenueBooking(bookingId, UUID.randomUUID(), "Sahne", 80, start, start.plusSeconds(3600))));
        create(organizer, venueEvent(bookingId)).andExpect(status().isCreated());

        // Son adım: unique ihlali PostgreSQL transaction'ını "aborted" yapar, sonrasında sorgu atılamaz.
        create(organizer, venueEvent(bookingId)).andExpect(status().isConflict());
    }

    // --- yardımcılar ---

    private String addressEvent(String visibility) {
        return """
                {"title": "Kitap Kulübü", "visibility": "%s", "customAddress": "Moda Parkı",
                 "startsAt": "%s", "endsAt": "%s"}
                """.formatted(visibility, offset(start), offset(start.plus(Duration.ofHours(2))));
    }

    private static String venueEvent(UUID bookingId) {
        return """
                {"title": "Sahne Gecesi", "visibility": "PUBLIC", "venueBookingId": "%s"}
                """.formatted(bookingId);
    }

    private ResultActions create(UUID userId, String json) throws Exception {
        return mvc.perform(post("/events").with(user(userId, "ORGANIZER"))
            .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions action(UUID userId, String eventId, String action) throws Exception {
        return mvc.perform(post("/events/{id}/{action}", eventId, action).with(user(userId, "ORGANIZER")));
    }

    private ResultActions bySlug(String slug) throws Exception {
        return mvc.perform(get("/events/by-slug/{slug}", slug));
    }

    private static String offset(Instant instant) {
        return instant.atOffset(ZoneOffset.ofHours(3)).toString();
    }

    private static RequestPostProcessor user(UUID userId, String role) {
        return jwt().jwt(j -> j.subject(userId.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
