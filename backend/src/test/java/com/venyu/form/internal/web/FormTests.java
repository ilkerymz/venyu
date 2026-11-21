package com.venyu.form.internal.web;

import com.venyu.EventFixtures;
import com.venyu.TestcontainersConfiguration;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static com.venyu.EventFixtures.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Event modülü gerçek (DIRECT_DEPENDENCIES): organizatör ve görünürlük kontrolü EventLookup üzerinden uçtan uca.
@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES, extraIncludes = "identity")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FormTests {

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

    @MockitoBean
    VenueBookings venueBookings; // event → venue bağımlılığı; bu testte mekanlı etkinlik yok

    UUID organizer = UUID.randomUUID();
    EventFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new EventFixtures(mvc, organizer);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM form_schemas");
        jdbc.update("DELETE FROM events");
    }

    @Test
    void everySaveCreatesANewVersionAndEmptyFieldsRemoveTheForm() throws Exception {
        String eventId = fixtures.draftEvent("PUBLIC", false);
        fixtures.publish(eventId);

        save(organizer, eventId, TRANSPORT_FORM).andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(1))
            .andExpect(jsonPath("$.fields[1].showIf").value("answers.transport == 'car'"))
            .andExpect(jsonPath("$.fields[0].ticketTypeIds").isEmpty()); // eksik alanlar normalize edildi
        save(organizer, eventId, TRANSPORT_FORM).andExpect(jsonPath("$.version").value(2));

        mvc.perform(get("/events/{id}/form", eventId)) // anonim
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(2))
            .andExpect(jsonPath("$.fields.length()").value(2));

        save(organizer, eventId, """
            {"fields": []}
            """).andExpect(jsonPath("$.version").doesNotExist());
        mvc.perform(get("/events/{id}/form", eventId))
            .andExpect(jsonPath("$.version").doesNotExist())
            .andExpect(jsonPath("$.fields").isEmpty());
    }

    @Test
    void onlyTheOrganizerCanSaveAndHiddenEventsHideTheirForm() throws Exception {
        String draft = fixtures.draftEvent("PUBLIC", false);
        String inviteOnly = fixtures.draftEvent("INVITE_ONLY", false);
        fixtures.publish(inviteOnly);

        save(UUID.randomUUID(), draft, TRANSPORT_FORM).andExpect(status().isForbidden()); // başka organizatör
        mvc.perform(put("/events/{id}/form", draft).with(user(organizer, "PARTICIPANT"))
                .contentType(MediaType.APPLICATION_JSON).content(TRANSPORT_FORM))
            .andExpect(status().isForbidden()); // rol yok
        save(organizer, UUID.randomUUID().toString(), TRANSPORT_FORM).andExpect(status().isNotFound());

        // Taslak ve INVITE_ONLY etkinliğin formu anonim ziyaretçiye 404, organizatöre 200.
        mvc.perform(get("/events/{id}/form", draft)).andExpect(status().isNotFound());
        mvc.perform(get("/events/{id}/form", inviteOnly)).andExpect(status().isNotFound());
        mvc.perform(get("/events/{id}/form", inviteOnly).with(user(organizer, "ORGANIZER")))
            .andExpect(status().isOk());
    }

    @Test
    void invalidSchemaReturnsAllErrorsAtOnce() throws Exception {
        String eventId = fixtures.draftEvent("PUBLIC", false);

        save(organizer, eventId, """
            {"fields": [
              {"key": "a", "label": "A", "type": "SINGLE_CHOICE"},
              {"key": "a", "label": "A tekrar", "type": "TEXT", "options": ["x"]},
              {"key": "b", "label": "B", "type": "TEXT", "showIf": "''.getClass() == null"},
              {"key": "c", "label": "C", "type": "TEXT", "ticketTypeIds": ["%s"]}
            ]}
            """.formatted(UUID.randomUUID()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.length()").value(5));
        // a: seçeneksiz | a: tekrar eden key | a: seçenekli olmayan alanda seçenek | b: metot çağrısı | c: yabancı bilet

        save(organizer, eventId, """
            {"fields": [{"key": "Bad Key", "label": "X", "type": "TEXT"}]}
            """).andExpect(status().isBadRequest()); // Bean Validation (key biçimi)
    }

    private ResultActions save(UUID organizerId, String eventId, String body) throws Exception {
        return mvc.perform(put("/events/{id}/form", eventId).with(user(organizerId, "ORGANIZER"))
            .contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
