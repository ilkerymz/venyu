package com.venyu.registration.internal.web;

import com.jayway.jsonpath.JsonPath;
import com.venyu.EventFixtures;
import com.venyu.TestcontainersConfiguration;
import com.venyu.registration.InvitationSent;
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

import java.util.UUID;

import static com.venyu.EventFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Form modülü ve event modülü gerçek (DIRECT_DEPENDENCIES): cevaplar kayıt anında uçtan uca doğrulanır.
@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES, extraIncludes = "identity")
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import(TestcontainersConfiguration.class)
class FormAnswersTests {

    static final String TRANSPORT_FORM = """
        {"fields": [
          {"key": "transport", "label": "Nasıl geleceksin?", "type": "SINGLE_CHOICE", "required": true,
           "options": ["car", "metro"]},
          {"key": "plate", "label": "Plakan", "type": "TEXT", "required": true,
           "showIf": "answers.transport == 'car'"}
        ]}
        """;

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ApplicationEvents events;

    @MockitoBean
    VenueBookings venueBookings; // event → venue bağımlılığı; bu testte mekanlı etkinlik yok

    UUID organizer = UUID.randomUUID();
    UUID participant = UUID.randomUUID();
    EventFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new EventFixtures(mvc, organizer);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM invitations");
        jdbc.update("DELETE FROM registrations");
        jdbc.update("DELETE FROM form_schemas");
        jdbc.update("DELETE FROM events");
    }

    @Test
    void conditionalRequiredFieldIsEnforcedAndHiddenAnswersAreDropped() throws Exception {
        Setup event = publishedEvent("PUBLIC", false);
        saveForm(event, TRANSPORT_FORM);

        register(participant, event, """
            {"transport": "car"}
            """).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors", hasItem("plate: zorunlu.")));
        assertThat(fixtures.sold(event.eventId(), event.ticketTypeId())).isZero(); // koltuk kilidine girilmedi

        String id = JsonPath.read(register(participant, event, """
            {"transport": "metro", "plate": "34ABC"}
            """).andExpect(status().isCreated())
            .andExpect(jsonPath("$.formVersion").value(1))
            .andReturn().getResponse().getContentAsString(), "$.id");

        String stored = storedAnswers(id);
        assertThat((String) JsonPath.read(stored, "$.transport")).isEqualTo("metro");
        assertThat(stored).doesNotContain("plate"); // gizli alanın cevabı saklanmadı
    }

    @Test
    void typeUnknownKeyAndFormlessEventErrorsAreRejected() throws Exception {
        Setup event = publishedEvent("PUBLIC", false);
        saveForm(event, TRANSPORT_FORM);

        register(participant, event, """
            {"transport": "bike", "extra": 1}
            """).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.length()").value(2)); // seçenek dışı + bilinmeyen alan

        Setup formless = publishedEvent("PUBLIC", false);
        register(participant, formless, """
            {"transport": "car"}
            """).andExpect(status().isBadRequest());
        register(participant, formless, null).andExpect(status().isCreated())
            .andExpect(jsonPath("$.formVersion").doesNotExist());
    }

    @Test
    void ticketSpecificQuestionIsAskedOnlyForThatTicket() throws Exception {
        Setup event = publishedEvent("PUBLIC", false);
        String vip = fixtures.ticket(event.eventId(), "VIP", 10);
        saveForm(event, """
            {"fields": [{"key": "tshirt", "label": "Tişört bedenin", "type": "SINGLE_CHOICE", "required": true,
                         "options": ["S", "M", "L"], "ticketTypeIds": ["%s"]}]}
            """.formatted(vip));

        register(participant, event, null).andExpect(status().isCreated()); // normal bilet: soru yok
        register(UUID.randomUUID(), new Setup(event.eventId(), vip), null)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors", hasItem("tshirt: zorunlu.")));
    }

    @Test
    void registrationsKeepTheVersionTheyWereValidatedAgainst() throws Exception {
        Setup event = publishedEvent("PUBLIC", false);
        saveForm(event, TRANSPORT_FORM);
        String first = JsonPath.read(register(participant, event, """
            {"transport": "metro"}
            """).andReturn().getResponse().getContentAsString(), "$.id");

        // v2: plaka artık herkese soruluyor.
        saveForm(event, """
            {"fields": [{"key": "plate", "label": "Plakan", "type": "TEXT", "required": true}]}
            """);
        register(UUID.randomUUID(), event, """
            {"transport": "metro"}
            """).andExpect(status().isBadRequest()); // v1'in cevabı v2'de geçersiz
        register(UUID.randomUUID(), event, """
            {"plate": "34ABC"}
            """).andExpect(jsonPath("$.formVersion").value(2));

        assertThat(jdbc.queryForObject("SELECT form_version FROM registrations WHERE id = ?::uuid",
            Integer.class, first)).isEqualTo(1);
    }

    @Test
    void guestAndInvitationPathsValidateBeforeTakingTheSeatOrSpendingTheInvitation() throws Exception {
        Setup event = publishedEvent("INVITE_ONLY", false);
        saveForm(event, TRANSPORT_FORM);
        mvc.perform(post("/events/{id}/invitations", event.eventId()).with(user(organizer, "ORGANIZER"))
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"emails": ["ayse@example.com"]}
                    """))
            .andExpect(status().isCreated());
        String token = events.stream(InvitationSent.class).toList().getLast().token();

        // INVITE_ONLY formu public GET'ten okunamaz; önizlemede döner.
        preview(token).andExpect(jsonPath("$.formVersion").value(1))
            .andExpect(jsonPath("$.formFields.length()").value(2));

        acceptAsGuest(token, event, """
            {"transport": "car"}
            """).andExpect(status().isBadRequest());
        preview(token).andExpect(jsonPath("$.status").value("PENDING")); // geçersiz cevap daveti harcamadı
        assertThat(fixtures.sold(event.eventId(), event.ticketTypeId())).isZero();

        acceptAsGuest(token, event, """
            {"transport": "car", "plate": "34ABC"}
            """).andExpect(status().isCreated())
            .andExpect(jsonPath("$.formVersion").value(1));
    }

    @Test
    void guestRegistrationWithInvalidAnswersTakesNoSeat() throws Exception {
        Setup event = publishedEvent("PUBLIC", false);
        saveForm(event, TRANSPORT_FORM);

        mvc.perform(post("/events/{id}/registrations/guest", event.eventId())
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"ticketTypeId": "%s", "email": "ayse@example.com", "name": "Ayşe", "answers": {}}
                    """.formatted(event.ticketTypeId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors", hasItem("transport: zorunlu.")));
        assertThat(fixtures.sold(event.eventId(), event.ticketTypeId())).isZero();
    }

    // --- kurulum ve istekler ---

    private record Setup(String eventId, String ticketTypeId) {
    }

    private Setup publishedEvent(String visibility, boolean requiresAccount) throws Exception {
        String eventId = fixtures.draftEvent(visibility, requiresAccount);
        String ticketTypeId = fixtures.ticket(eventId, "Bilet", 10);
        fixtures.publish(eventId);
        return new Setup(eventId, ticketTypeId);
    }

    private void saveForm(Setup event, String body) throws Exception {
        mvc.perform(put("/events/{id}/form", event.eventId()).with(user(organizer, "ORGANIZER"))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());
    }

    // answers null ise alan hiç gönderilmez.
    private ResultActions register(UUID userId, Setup event, String answers) throws Exception {
        String body = answers == null
            ? "{\"ticketTypeId\": \"%s\"}".formatted(event.ticketTypeId())
            : "{\"ticketTypeId\": \"%s\", \"answers\": %s}".formatted(event.ticketTypeId(), answers.strip());
        return mvc.perform(post("/events/{id}/registrations", event.eventId()).with(user(userId, "PARTICIPANT"))
            .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions preview(String token) throws Exception {
        return mvc.perform(post("/invitations/preview").contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\": \"%s\"}".formatted(token)));
    }

    private ResultActions acceptAsGuest(String token, Setup event, String answers) throws Exception {
        return mvc.perform(post("/invitations/accept/guest").contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\": \"%s\", \"ticketTypeId\": \"%s\", \"name\": \"Ayşe\", \"answers\": %s}"
                .formatted(token, event.ticketTypeId(), answers.strip())));
    }

    private String storedAnswers(String registrationId) {
        return jdbc.queryForObject("SELECT answers::text FROM registrations WHERE id = ?::uuid",
            String.class, registrationId);
    }
}
