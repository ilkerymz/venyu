package com.venyu.registration.internal.web;

import com.jayway.jsonpath.JsonPath;
import com.venyu.EventFixtures;
import com.venyu.TestcontainersConfiguration;
import com.venyu.registration.GuestConfirmationRequested;
import com.venyu.registration.TicketIssued;
import com.venyu.registration.internal.service.AttendeeService;
import com.venyu.venue.VenueBookings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.ErrorResponseException;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.venyu.EventFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Event, form ve identity gerçek: bilet kodu, check-in, katılımcı listesi ve "yönet" link'i uçtan uca.
// Bilinçli olarak @Transactional değil (eşzamanlı check-in).
@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES, extraIncludes = "identity")
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import(TestcontainersConfiguration.class)
class TicketAndCheckInTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ApplicationEvents events;

    @Autowired
    AttendeeService attendeeService;

    @MockitoBean
    VenueBookings venueBookings; // event → venue bağımlılığı; bu testte mekanlı etkinlik yok

    UUID organizer = UUID.randomUUID();
    UUID member = UUID.randomUUID();
    // Check-in penceresi (başlangıçtan 3 saat önce) şu an açık olsun.
    Instant soon = Instant.now().plus(Duration.ofHours(1)).truncatedTo(ChronoUnit.MINUTES);
    EventFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new EventFixtures(mvc, organizer);
        // Katılımcı listesinde adın identity'den geldiğini görmek için gerçek kullanıcı satırı.
        jdbc.update("INSERT INTO users (id, email, password_hash, display_name, created_at) "
            + "VALUES (?, ?, 'x', 'Mehmet', now())", member, member + "@example.com");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM registrations");
        jdbc.update("DELETE FROM form_schemas");
        jdbc.update("DELETE FROM events");
        jdbc.update("DELETE FROM users WHERE id = ?", member);
    }

    @Test
    void memberGetsATicketThatChecksInExactlyOnce() throws Exception {
        Setup event = publishedEvent(soon);
        String code = JsonPath.read(register(member, event).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString(), "$.ticketCode");
        assertThat(code).hasSize(22); // 16 byte, base64url

        checkIn(organizer, event, code).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CHECKED_IN"))
            .andExpect(jsonPath("$.name").value("Mehmet"))
            .andExpect(jsonPath("$.checkedInAt").exists());
        checkIn(organizer, event, code).andExpect(status().isConflict())
            .andExpect(jsonPath("$.checkedInAt").exists()); // görevli ne zaman girildiğini görür

        // Başka etkinliğin kapısında bu bilet geçmez; uydurma kod da.
        Setup other = publishedEvent(soon);
        checkIn(organizer, other, code).andExpect(status().isNotFound());
        checkIn(organizer, event, "uydurma-kod").andExpect(status().isNotFound());

        mvc.perform(get("/me/registrations").with(user(member, "PARTICIPANT")))
            .andExpect(jsonPath("$[0].status").value("CHECKED_IN"));
    }

    @Test
    void sameTicketScannedAtTwentyGatesAtOnceChecksInOnce() throws Exception {
        Setup event = publishedEvent(soon);
        String code = JsonPath.read(register(member, event).andReturn().getResponse().getContentAsString(),
            "$.ticketCode");
        UUID eventId = UUID.fromString(event.eventId());

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(() -> succeeds(() -> attendeeService.checkIn(organizer, eventId, code)));
        }

        assertThat(runConcurrently(tasks)).isEqualTo(1);
    }

    @Test
    void checkInRespectsTheWindowTicketStateAndOwnership() throws Exception {
        Setup later = publishedEvent(Instant.now().plus(Duration.ofDays(7)));
        String laterCode = JsonPath.read(register(member, later).andReturn().getResponse().getContentAsString(),
            "$.ticketCode");
        checkIn(organizer, later, laterCode).andExpect(status().isConflict()); // kapı henüz açılmadı

        Setup event = publishedEvent(soon);
        String body = register(member, event).andReturn().getResponse().getContentAsString();
        String code = JsonPath.read(body, "$.ticketCode");

        checkIn(UUID.randomUUID(), event, code).andExpect(status().isForbidden()); // başka organizatör
        mvc.perform(post("/events/{id}/check-ins", event.eventId()).with(user(member, "PARTICIPANT"))
                .contentType(MediaType.APPLICATION_JSON).content(checkInJson(code)))
            .andExpect(status().isForbidden()); // katılımcı kendi biletini okutamaz

        mvc.perform(post("/registrations/{id}/cancel", (String) JsonPath.read(body, "$.id"))
                .with(user(member, "PARTICIPANT")))
            .andExpect(status().isOk());
        checkIn(organizer, event, code).andExpect(status().isConflict()); // iptal edilmiş bilet
    }

    @Test
    void guestReceivesTicketAndManageLinkThatCanCancelAndFreeTheSeat() throws Exception {
        Setup event = publishedEvent(soon);
        mvc.perform(post("/events/{id}/registrations/guest", event.eventId())
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"ticketTypeId": "%s", "email": "ayse@example.com", "name": "Ayşe"}
                    """.formatted(event.ticketTypeId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ticketCode").doesNotExist()); // onaysız kayda bilet yok
        String confirmToken = events.stream(GuestConfirmationRequested.class).toList().getLast().token();
        mvc.perform(post("/registrations/confirm").contentType(MediaType.APPLICATION_JSON)
                .content(tokenJson(confirmToken)))
            .andExpect(jsonPath("$.ticketCode").exists());

        TicketIssued ticket = events.stream(TicketIssued.class).toList().getLast();
        assertThat(ticket.email()).isEqualTo("ayse@example.com");

        manage("/registrations/manage", ticket.manageToken()).andExpect(status().isOk())
            .andExpect(jsonPath("$.ticketCode").value(ticket.ticketCode()))
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
        assertThat(fixtures.sold(event.eventId(), event.ticketTypeId())).isEqualTo(1);

        // Bilet kodu yönetim yetkisi vermez: kapıdaki görevli kodu görse de iptal edemez.
        manage("/registrations/manage/cancel", ticket.ticketCode()).andExpect(status().isBadRequest());

        manage("/registrations/manage/cancel", ticket.manageToken()).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(fixtures.sold(event.eventId(), event.ticketTypeId())).isZero(); // koltuk bırakıldı
        manage("/registrations/manage/cancel", ticket.manageToken()).andExpect(status().isConflict());
        checkIn(organizer, event, ticket.ticketCode()).andExpect(status().isConflict());
    }

    @Test
    void organizerSeesAttendeesWithContactsAndAnswers() throws Exception {
        Setup event = publishedEvent(soon);
        mvc.perform(put("/events/{id}/form", event.eventId()).with(user(organizer, "ORGANIZER"))
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"fields": [{"key": "note", "label": "Not", "type": "TEXT"}]}
                    """))
            .andExpect(status().isOk());

        mvc.perform(post("/events/{id}/registrations", event.eventId()).with(user(member, "PARTICIPANT"))
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"ticketTypeId": "%s", "answers": {"note": "Vejetaryenim"}}
                    """.formatted(event.ticketTypeId())))
            .andExpect(status().isCreated());
        // Onaylanmayıp süresi dolan misafir listede görünmez.
        mvc.perform(post("/events/{id}/registrations/guest", event.eventId())
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"ticketTypeId": "%s", "email": "ayse@example.com", "name": "Ayşe"}
                """.formatted(event.ticketTypeId())));
        jdbc.update("UPDATE registrations SET status = 'EXPIRED' WHERE guest_email = 'ayse@example.com'");

        mvc.perform(get("/events/{id}/registrations", event.eventId()).with(user(organizer, "ORGANIZER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Mehmet"))
            .andExpect(jsonPath("$[0].email").value(member + "@example.com"))
            .andExpect(jsonPath("$[0].guest").value(false))
            .andExpect(jsonPath("$[0].answers.note").value("Vejetaryenim"))
            .andExpect(jsonPath("$[0].ticketCode").doesNotExist());

        mvc.perform(get("/events/{id}/registrations", event.eventId()).with(user(UUID.randomUUID(), "ORGANIZER")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/events/{id}/registrations", event.eventId()).with(user(member, "PARTICIPANT")))
            .andExpect(status().isForbidden());
    }

    // --- kurulum ve istekler ---

    private record Setup(String eventId, String ticketTypeId) {
    }

    private Setup publishedEvent(Instant startsAt) throws Exception {
        String eventId = fixtures.draftEvent("PUBLIC", false, startsAt);
        String ticketTypeId = fixtures.ticket(eventId, "Bilet", 10);
        fixtures.publish(eventId);
        return new Setup(eventId, ticketTypeId);
    }

    private ResultActions register(UUID userId, Setup event) throws Exception {
        return mvc.perform(post("/events/{id}/registrations", event.eventId()).with(user(userId, "PARTICIPANT"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"ticketTypeId\": \"%s\"}".formatted(event.ticketTypeId())));
    }

    private ResultActions checkIn(UUID organizerId, Setup event, String code) throws Exception {
        return mvc.perform(post("/events/{id}/check-ins", event.eventId()).with(user(organizerId, "ORGANIZER"))
            .contentType(MediaType.APPLICATION_JSON).content(checkInJson(code)));
    }

    private ResultActions manage(String path, String token) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(tokenJson(token)));
    }

    private static String checkInJson(String code) {
        return "{\"ticketCode\": \"%s\"}".formatted(code);
    }

    private static String tokenJson(String token) {
        return "{\"token\": \"%s\"}".formatted(token);
    }

    // --- eşzamanlılık (RegistrationTests ile aynı) ---

    private static boolean succeeds(Runnable action) {
        try {
            action.run();
            return true;
        } catch (ErrorResponseException e) {
            return false; // AlreadyCheckedInException
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
}
