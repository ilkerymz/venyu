package com.venyu.registration.internal.web;

import com.jayway.jsonpath.JsonPath;
import com.venyu.TestcontainersConfiguration;
import com.venyu.registration.internal.service.RegistrationService;
import com.venyu.venue.VenueBookings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.ErrorResponseException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Event modülü GERÇEK (DIRECT_DEPENDENCIES): modüller arası atomiklik ve overselling uçtan uca test edilir.
// Etkinlikler event'in public HTTP API'siyle kurulur. Bilinçli olarak @Transactional değil (eşzamanlılık).
@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES, extraIncludes = "identity")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RegistrationTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    RegistrationService registrationService;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    VenueBookings venueBookings; // event → venue bağımlılığı; bu testte mekanlı etkinlik yok

    UUID organizer = UUID.randomUUID();
    UUID participant = UUID.randomUUID();
    Instant start = Instant.now().plus(Duration.ofDays(7)).truncatedTo(ChronoUnit.HOURS);

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM registrations");
        jdbc.update("DELETE FROM events"); // ticket_types FK cascade ile
    }

    @Test
    void participantRegistersForFreeTicket() throws Exception {
        Setup event = publishedEvent("PUBLIC", "0", 10);

        register(participant, event).andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mvc.perform(get("/me/registrations").with(user(participant, "PARTICIPANT")))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].eventId").value(event.eventId()))
            .andExpect(jsonPath("$[0].event.title").value("Test"))
            .andExpect(jsonPath("$[0].event.slug").isNotEmpty())
            .andExpect(jsonPath("$[0].event.customAddress").value("Moda"))
            .andExpect(jsonPath("$[0].event.cancelled").value(false))
            .andExpect(jsonPath("$[0].event.publiclyVisible").value(true));
        assertThat(sold(event)).isEqualTo(1);
    }

    @Test
    void myRegistrationsShowThatTheEventWasCancelled() throws Exception {
        Setup event = publishedEvent("PUBLIC", "0", 10);
        register(participant, event).andExpect(status().isCreated());

        mvc.perform(post("/events/{id}/cancel", event.eventId()).with(user(organizer, "ORGANIZER")))
            .andExpect(status().isOk());

        // Kaydın kendisi async listener'la iptal edilir; burada etkinliğin durumuna bakıyoruz (anında).
        mvc.perform(get("/me/registrations").with(user(participant, "PARTICIPANT")))
            .andExpect(jsonPath("$[0].event.cancelled").value(true));
    }

    @Test
    void duplicateAndSoldOutAreRejected() throws Exception {
        Setup event = publishedEvent("PUBLIC", "0", 1);
        register(participant, event).andExpect(status().isCreated());

        register(participant, event).andExpect(status().isConflict());           // zaten kayıtlı
        register(UUID.randomUUID(), event).andExpect(status().isConflict());     // tükendi
        assertThat(sold(event)).isEqualTo(1);
    }

    @Test
    void cancellingReleasesTheSeatForSomeoneElse() throws Exception {
        Setup event = publishedEvent("PUBLIC", "0", 1);
        String registrationId = JsonPath.read(register(participant, event)
            .andReturn().getResponse().getContentAsString(), "$.id");

        // IDOR: başkasının kaydı iptal edilemez.
        cancel(UUID.randomUUID(), registrationId).andExpect(status().isNotFound());

        cancel(participant, registrationId).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
        cancel(participant, registrationId).andExpect(status().isConflict());
        assertThat(sold(event)).isZero();

        register(UUID.randomUUID(), event).andExpect(status().isCreated());
        register(participant, event).andExpect(status().isConflict()); // koltuk tekrar doldu
    }

    @Test
    void unavailableTargetsAreRejected() throws Exception {
        register(participant, publishedEvent("PUBLIC", "100", 10)).andExpect(status().isConflict());  // ücretli
        register(participant, publishedEvent("INVITE_ONLY", "0", 10)).andExpect(status().isConflict()); // davetli
        register(participant, draftEvent()).andExpect(status().isConflict());                          // taslak

        Setup event = publishedEvent("PUBLIC", "0", 10);
        register(participant, new Setup(event.eventId(), UUID.randomUUID().toString()))
            .andExpect(status().isNotFound());

        mvc.perform(post("/events/{id}/registrations", event.eventId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationJson(event.ticketTypeId())))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void twoHundredParticipantsCompeteForTheLastSeatAndExactlyOneWins() throws Exception {
        Setup event = publishedEvent("PUBLIC", "0", 1);
        UUID eventId = UUID.fromString(event.eventId());
        UUID ticketId = UUID.fromString(event.ticketTypeId());

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            UUID someone = UUID.randomUUID();
            tasks.add(() -> succeeds(() -> registrationService.register(someone, eventId, ticketId, Map.of())));
        }

        assertThat(runConcurrently(tasks)).isEqualTo(1);
        assertThat(sold(event)).isEqualTo(1);
    }

    @Test
    void doubleClickByTheSameUserTakesOnlyOneSeat() throws Exception {
        Setup event = publishedEvent("PUBLIC", "0", 10);
        UUID eventId = UUID.fromString(event.eventId());
        UUID ticketId = UUID.fromString(event.ticketTypeId());

        List<Callable<Boolean>> tasks = List.of(
            () -> succeeds(() -> registrationService.register(participant, eventId, ticketId, Map.of())),
            () -> succeeds(() -> registrationService.register(participant, eventId, ticketId, Map.of())));

        assertThat(runConcurrently(tasks)).isEqualTo(1);
        // Kaybeden istek de koltuk ayırmıştı; unique ihlali transaction'ı geri alınca koltuk da geri alındı.
        assertThat(sold(event)).isEqualTo(1);
    }

    // --- kurulum (event modülünün public HTTP API'si üzerinden) ---

    private record Setup(String eventId, String ticketTypeId) {
    }

    private Setup publishedEvent(String visibility, String price, int capacity) throws Exception {
        Setup setup = draftEvent(visibility, price, capacity);
        mvc.perform(post("/events/{id}/publish", setup.eventId()).with(user(organizer, "ORGANIZER")))
            .andExpect(status().isOk());
        return setup;
    }

    private Setup draftEvent() throws Exception {
        return draftEvent("PUBLIC", "0", 10);
    }

    private Setup draftEvent(String visibility, String price, int capacity) throws Exception {
        String eventId = JsonPath.read(mvc.perform(post("/events").with(user(organizer, "ORGANIZER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {"title": "Test", "visibility": "%s", "customAddress": "Moda",
                                 "startsAt": "%s", "endsAt": "%s"}
                                """.formatted(visibility, start.atOffset(ZoneOffset.UTC),
                    start.plus(Duration.ofHours(2)).atOffset(ZoneOffset.UTC))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString(), "$.id");
        String ticketTypeId = JsonPath.read(mvc.perform(post("/events/{id}/ticket-types", eventId)
                .with(user(organizer, "ORGANIZER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {"name": "Bilet", "price": %s, "capacity": %d}
                                """.formatted(price, capacity)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString(), "$.id");
        return new Setup(eventId, ticketTypeId);
    }

    private int sold(Setup event) throws Exception {
        String body = mvc.perform(get("/events/{id}/ticket-types", event.eventId()).with(user(organizer, "ORGANIZER")))
            .andReturn().getResponse().getContentAsString();
        List<Integer> sold = JsonPath.read(body, "$[?(@.id == '" + event.ticketTypeId() + "')].sold");
        return sold.getFirst();
    }

    // --- istekler ---

    private ResultActions register(UUID userId, Setup event) throws Exception {
        return mvc.perform(post("/events/{id}/registrations", event.eventId()).with(user(userId, "PARTICIPANT"))
            .contentType(MediaType.APPLICATION_JSON).content(registrationJson(event.ticketTypeId())));
    }

    private ResultActions cancel(UUID userId, String registrationId) throws Exception {
        return mvc.perform(post("/registrations/{id}/cancel", registrationId).with(user(userId, "PARTICIPANT")));
    }

    private static String registrationJson(String ticketTypeId) {
        return """
                {"ticketTypeId": "%s"}
                """.formatted(ticketTypeId);
    }

    // --- eşzamanlılık ---

    private static boolean succeeds(Runnable action) {
        try {
            action.run();
            return true;
        } catch (ErrorResponseException e) {
            return false; // SoldOutException / AlreadyRegisteredException
        }
    }

    private static int runConcurrently(List<Callable<Boolean>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (Callable<Boolean> task : tasks) {
            results.add(executor.submit(() -> {
                start.await();
                return task.call();
            }));
        }
        start.countDown();

        int winners = 0;
        for (Future<Boolean> result : results) {
            if (result.get(30, TimeUnit.SECONDS)) {
                winners++;
            }
        }
        executor.shutdown();
        return winners;
    }

    private static RequestPostProcessor user(UUID userId, String role) {
        return jwt().jwt(j -> j.subject(userId.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
