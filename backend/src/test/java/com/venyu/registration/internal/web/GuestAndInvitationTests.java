package com.venyu.registration.internal.web;

import com.jayway.jsonpath.JsonPath;
import com.venyu.TestcontainersConfiguration;
import com.venyu.registration.GuestConfirmationRequested;
import com.venyu.registration.InvitationSent;
import com.venyu.registration.internal.service.InvitationService;
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
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.ErrorResponseException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// RegistrationTests ile aynı kurulum: event modülü gerçek, etkinlikler HTTP ile kurulur, @Transactional değil.
// Ham token'lar yayınlanan event'lerden alınır (@RecordApplicationEvents, F1'deki desen).
@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES, extraIncludes = "identity")
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import(TestcontainersConfiguration.class)
class GuestAndInvitationTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    RegistrationService registrationService;

    @Autowired
    InvitationService invitationService;

    @Autowired
    ApplicationEvents events;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    VenueBookings venueBookings; // event → venue bağımlılığı; bu testte mekanlı etkinlik yok

    UUID organizer = UUID.randomUUID();
    UUID participant = UUID.randomUUID();
    Instant start = Instant.now().plus(Duration.ofDays(7)).truncatedTo(ChronoUnit.HOURS);

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM invitations");
        jdbc.update("DELETE FROM registrations");
        jdbc.update("DELETE FROM events"); // ticket_types FK cascade ile
    }

    // --- misafir kaydı ---

    @Test
    void guestRegistersAndConfirmsWithSingleUseLink() throws Exception {
        Setup event = publishedEvent("PUBLIC", false, 10);

        registerGuest(event, "Ayse@Example.com").andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("RESERVED"))
            .andExpect(jsonPath("$.expiresAt").exists());
        assertThat(sold(event)).isEqualTo(1); // onay beklerken koltuk tutuluyor

        // E-posta normalize edildiği için büyük/küçük harf farkı ikinci kayıt açmaz.
        registerGuest(event, "ayse@example.com").andExpect(status().isConflict());

        String token = lastGuestToken();
        confirm(token).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.expiresAt").doesNotExist());
        confirm(token).andExpect(status().isBadRequest()); // tek kullanımlık
        assertThat(sold(event)).isEqualTo(1);
    }

    @Test
    void unconfirmedGuestExpiresAndTheSeatIsReleased() throws Exception {
        Setup event = publishedEvent("PUBLIC", false, 1);
        registerGuest(event, "ayse@example.com").andExpect(status().isCreated());
        String token = lastGuestToken();
        registerGuest(event, "mehmet@example.com").andExpect(status().isConflict()); // tükendi

        // Zamanı ilerletmek yerine süreyi geçmişe çek; job mantığını servis üzerinden çalıştır (F1 deseni).
        jdbc.update("UPDATE registrations SET expires_at = now() - interval '1 minute'");
        assertThat(registrationService.expireReservations()).isEqualTo(1);
        assertThat(registrationService.expireReservations()).isZero(); // ikinci tur boş

        assertThat(sold(event)).isZero();
        confirm(token).andExpect(status().isConflict()); // "süre doldu", "geçersiz link" değil
        registerGuest(event, "mehmet@example.com").andExpect(status().isCreated());
        registerGuest(event, "ayse@example.com").andExpect(status().isConflict()); // koltuk tekrar doldu
    }

    @Test
    void guestRegistrationFollowsEventRules() throws Exception {
        registerGuest(publishedEvent("PUBLIC", true, 10), "ayse@example.com")
            .andExpect(status().isConflict());   // hesap gerekli
        registerGuest(publishedEvent("INVITE_ONLY", false, 10), "ayse@example.com")
            .andExpect(status().isConflict());   // sadece davetle
        registerGuest(publishedEvent("PUBLIC", false, 10), "e-posta-degil")
            .andExpect(status().isBadRequest()); // validation
        confirm("uydurma-token").andExpect(status().isBadRequest());
    }

    // --- davetler ---

    @Test
    void organizerInvitesIdempotentlyAndOnlyTheOrganizerCanManageInvitations() throws Exception {
        Setup event = publishedEvent("INVITE_ONLY", false, 10);

        invite(organizer, event, "a@example.com", "A@Example.com", "b@example.com")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.length()").value(2)); // aynı e-posta tek davet
        invite(organizer, event, "b@example.com", "c@example.com")
            .andExpect(jsonPath("$.length()").value(1)); // b zaten davetli, atlandı
        assertThat(events.stream(InvitationSent.class)).hasSize(3);

        mvc.perform(get("/events/{id}/invitations", event.eventId()).with(user(organizer, "ORGANIZER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].status").value("PENDING"));

        // Başka organizatör (IDOR) ve organizatör olmayan kullanıcı.
        invite(UUID.randomUUID(), event, "d@example.com").andExpect(status().isForbidden());
        mvc.perform(get("/events/{id}/invitations", event.eventId()).with(user(UUID.randomUUID(), "ORGANIZER")))
            .andExpect(status().isForbidden());
        mvc.perform(post("/events/{id}/invitations", event.eventId()).with(user(organizer, "PARTICIPANT"))
                .contentType(MediaType.APPLICATION_JSON).content(invitationJson("d@example.com")))
            .andExpect(status().isForbidden());
    }

    @Test
    void invitedGuestPreviewsAndJoinsInviteOnlyEventWithoutEmailConfirmation() throws Exception {
        Setup event = publishedEvent("INVITE_ONLY", false, 10);
        String token = inviteOne(event, "ayse@example.com");

        preview(token).andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("ayse@example.com"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.eventTitle").value("Test"))
            .andExpect(jsonPath("$.tickets.length()").value(2)); // varsayılan + test bileti

        acceptAsGuest(token, event).andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
        acceptAsGuest(token, event).andExpect(status().isConflict()); // davet tek kullanımlık
        preview(token).andExpect(jsonPath("$.status").value("ACCEPTED"));
        assertThat(sold(event)).isEqualTo(1);

        register(participant, event).andExpect(status().isConflict()); // davetsiz üye giremez
        preview("uydurma-token").andExpect(status().isBadRequest());
    }

    @Test
    void invitedMemberAcceptsWithAccount() throws Exception {
        Setup event = publishedEvent("INVITE_ONLY", true, 10);
        String token = inviteOne(event, "ayse@example.com");

        acceptAsGuest(token, event).andExpect(status().isConflict()); // hesap gerekli
        preview(token).andExpect(jsonPath("$.status").value("PENDING")); // başarısız kabul daveti harcamadı

        mvc.perform(post("/invitations/accept").contentType(MediaType.APPLICATION_JSON)
                .content(acceptJson(token, event)))
            .andExpect(status().isUnauthorized());
        acceptAsMember(participant, token, event).andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mvc.perform(get("/me/registrations").with(user(participant, "PARTICIPANT")))
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void soldOutAcceptLeavesTheInvitationUsable() throws Exception {
        Setup event = publishedEvent("INVITE_ONLY", false, 1);
        String first = inviteOne(event, "a@example.com");
        String second = inviteOne(event, "b@example.com");

        acceptAsGuest(first, event).andExpect(status().isCreated());
        acceptAsGuest(second, event).andExpect(status().isConflict()); // tükendi

        // Davet önce ACCEPTED yapılmıştı; hata transaction'ı geri alınca davet de PENDING'e döndü.
        preview(second).andExpect(jsonPath("$.status").value("PENDING"));
        assertThat(sold(event)).isEqualTo(1);
    }

    @Test
    void sameInvitationAcceptedConcurrentlyTakesOnlyOneSeat() throws Exception {
        Setup event = publishedEvent("INVITE_ONLY", false, 10);
        String token = inviteOne(event, "ayse@example.com");
        UUID ticketId = UUID.fromString(event.ticketTypeId());

        // Link iletildi: 10 farklı kullanıcı aynı daveti aynı anda kabul etmeye çalışır.
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UUID someone = UUID.randomUUID();
            tasks.add(() -> succeeds(() -> invitationService.acceptAsMember(someone, token, ticketId, Map.of())));
        }

        assertThat(runConcurrently(tasks)).isEqualTo(1);
        assertThat(sold(event)).isEqualTo(1);
    }

    // --- kurulum (event modülünün public HTTP API'si üzerinden) ---

    private record Setup(String eventId, String ticketTypeId) {
    }

    private Setup publishedEvent(String visibility, boolean requiresAccount, int capacity) throws Exception {
        String eventId = JsonPath.read(mvc.perform(post("/events").with(user(organizer, "ORGANIZER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Test", "visibility": "%s", "requiresAccount": %s, "customAddress": "Moda",
                     "startsAt": "%s", "endsAt": "%s"}
                    """.formatted(visibility, requiresAccount, start.atOffset(ZoneOffset.UTC),
                    start.plus(Duration.ofHours(2)).atOffset(ZoneOffset.UTC))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString(), "$.id");
        String ticketTypeId = JsonPath.read(mvc.perform(post("/events/{id}/ticket-types", eventId)
                .with(user(organizer, "ORGANIZER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "Bilet", "price": 0, "capacity": %d}
                    """.formatted(capacity)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(post("/events/{id}/publish", eventId).with(user(organizer, "ORGANIZER")))
            .andExpect(status().isOk());
        return new Setup(eventId, ticketTypeId);
    }

    private int sold(Setup event) throws Exception {
        String body = mvc.perform(get("/events/{id}/ticket-types", event.eventId()).with(user(organizer, "ORGANIZER")))
            .andReturn().getResponse().getContentAsString();
        List<Integer> sold = JsonPath.read(body, "$[?(@.id == '" + event.ticketTypeId() + "')].sold");
        return sold.getFirst();
    }

    private String lastGuestToken() {
        return events.stream(GuestConfirmationRequested.class).toList().getLast().token();
    }

    private String inviteOne(Setup event, String email) throws Exception {
        invite(organizer, event, email).andExpect(status().isCreated());
        return events.stream(InvitationSent.class).filter(sent -> sent.email().equals(email))
            .toList().getLast().token();
    }

    // --- istekler ---

    private ResultActions registerGuest(Setup event, String email) throws Exception {
        return mvc.perform(post("/events/{id}/registrations/guest", event.eventId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"ticketTypeId": "%s", "email": "%s", "name": "Ayşe"}
                """.formatted(event.ticketTypeId(), email)));
    }

    private ResultActions confirm(String token) throws Exception {
        return mvc.perform(post("/registrations/confirm").contentType(MediaType.APPLICATION_JSON)
            .content(tokenJson(token)));
    }

    private ResultActions register(UUID userId, Setup event) throws Exception {
        return mvc.perform(post("/events/{id}/registrations", event.eventId()).with(user(userId, "PARTICIPANT"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"ticketTypeId": "%s"}
                """.formatted(event.ticketTypeId())));
    }

    private ResultActions invite(UUID organizerId, Setup event, String... emails) throws Exception {
        return mvc.perform(post("/events/{id}/invitations", event.eventId()).with(user(organizerId, "ORGANIZER"))
            .contentType(MediaType.APPLICATION_JSON).content(invitationJson(emails)));
    }

    private ResultActions preview(String token) throws Exception {
        return mvc.perform(post("/invitations/preview").contentType(MediaType.APPLICATION_JSON)
            .content(tokenJson(token)));
    }

    private ResultActions acceptAsGuest(String token, Setup event) throws Exception {
        return mvc.perform(post("/invitations/accept/guest").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"token": "%s", "ticketTypeId": "%s", "name": "Ayşe"}
                """.formatted(token, event.ticketTypeId())));
    }

    private ResultActions acceptAsMember(UUID userId, String token, Setup event) throws Exception {
        return mvc.perform(post("/invitations/accept").with(user(userId, "PARTICIPANT"))
            .contentType(MediaType.APPLICATION_JSON).content(acceptJson(token, event)));
    }

    private static String invitationJson(String... emails) {
        return """
            {"emails": [%s]}
            """.formatted(Arrays.stream(emails).map(email -> "\"" + email + "\"").collect(Collectors.joining(", ")));
    }

    private static String tokenJson(String token) {
        return """
            {"token": "%s"}
            """.formatted(token);
    }

    private static String acceptJson(String token, Setup event) {
        return """
            {"token": "%s", "ticketTypeId": "%s"}
            """.formatted(token, event.ticketTypeId());
    }

    // --- eşzamanlılık (RegistrationTests ile aynı) ---

    private static boolean succeeds(Runnable action) {
        try {
            action.run();
            return true;
        } catch (ErrorResponseException e) {
            return false; // InvitationConflictException / RegistrationStateException
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
