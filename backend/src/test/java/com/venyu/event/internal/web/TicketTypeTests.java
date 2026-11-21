package com.venyu.event.internal.web;

import com.jayway.jsonpath.JsonPath;
import com.venyu.TestcontainersConfiguration;
import com.venyu.event.internal.repository.TicketTypeRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ApplicationModuleTest(extraIncludes = "identity")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class TicketTypeTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    TicketTypeRepository tickets;

    @MockitoBean
    VenueBookings venueBookings;

    UUID organizer = UUID.randomUUID();
    Instant start = Instant.now().plus(Duration.ofDays(7)).truncatedTo(ChronoUnit.HOURS);

    @Test
    void addressEventGetsFreeUnlimitedDefaultTicket() throws Exception {
        String eventId = createAddressEvent();

        listTickets(eventId)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Genel Katılım"))
            .andExpect(jsonPath("$[0].price").value(0.0))
            .andExpect(jsonPath("$[0].capacity").doesNotExist());
    }

    @Test
    void venueEventDefaultTicketUsesVenueCapacityAndTotalCannotExceedIt() throws Exception {
        String eventId = createVenueEvent(80);
        String defaultTicket = JsonPath.read(listTickets(eventId)
            .andExpect(jsonPath("$[0].capacity").value(80))
            .andReturn().getResponse().getContentAsString(), "$[0].id");

        addTicket(eventId, "Ek", "0", 10).andExpect(status().isBadRequest());
        addTicket(eventId, "Sınırsız", "0", null).andExpect(status().isBadRequest());

        updateTicket(eventId, defaultTicket, "Genel Katılım", "0", 50).andExpect(status().isOk());
        addTicket(eventId, "VIP", "250", 30)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.price").value(250.0));
        addTicket(eventId, "Fazla", "0", 1).andExpect(status().isBadRequest());
    }

    @Test
    void addressEventAllowsUnlimitedAndPaidTickets() throws Exception {
        String eventId = createAddressEvent();

        addTicket(eventId, "Destekçi", "99.90", null)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.price").value(99.9))
            .andExpect(jsonPath("$.remaining").doesNotExist());
    }

    @Test
    void invalidPricesAreRejected() throws Exception {
        String eventId = createAddressEvent();

        addTicket(eventId, "Negatif", "-1", null).andExpect(status().isBadRequest());
        addTicket(eventId, "Üç basamak", "10.999", null).andExpect(status().isBadRequest());
    }

    @Test
    void lastTicketAndTicketsWithSalesCannotBeDeleted() throws Exception {
        String eventId = createAddressEvent();
        String defaultTicket = firstTicketId(eventId);

        deleteTicket(eventId, defaultTicket).andExpect(status().isConflict());

        String vip = JsonPath.read(addTicket(eventId, "VIP", "100", 10)
            .andReturn().getResponse().getContentAsString(), "$.id");
        tickets.reserveSeat(UUID.fromString(vip));
        tickets.flush();

        deleteTicket(eventId, vip).andExpect(status().isConflict());
        updateTicket(eventId, vip, "VIP", "120", 10).andExpect(status().isBadRequest()); // satış var: fiyat sabit
        deleteTicket(eventId, defaultTicket).andExpect(status().isNoContent());
    }

    @Test
    void ticketsAreManagedOnlyByTheOrganizerAndOnlyWithinTheirEvent() throws Exception {
        String myEvent = createAddressEvent();
        String myTicket = firstTicketId(myEvent);
        UUID other = UUID.randomUUID();

        addTicketAs(other, myEvent).andExpect(status().isForbidden());

        // IDOR: kendi etkinliğinin URL'i üzerinden başka etkinliğin biletine erişilemez.
        String otherEvent = createAddressEventAs(other);
        mvc.perform(put("/events/{e}/ticket-types/{t}", otherEvent, myTicket).with(user(other))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ticketJson("Ele geçirildi", "0", null)))
            .andExpect(status().isNotFound());
    }

    @Test
    void cancelledEventTicketsCannotBeChanged() throws Exception {
        String eventId = createAddressEvent();
        mvc.perform(post("/events/{id}/cancel", eventId).with(user(organizer))).andExpect(status().isOk());

        addTicket(eventId, "Geç", "0", null).andExpect(status().isConflict());
    }

    @Test
    void publicViewShowsTicketsButHidesInternalData() throws Exception {
        String body = create(organizer, addressEventJson()).andReturn().getResponse().getContentAsString();
        String eventId = JsonPath.read(body, "$.id");
        String vip = JsonPath.read(addTicket(eventId, "VIP", "100", 1)
            .andReturn().getResponse().getContentAsString(), "$.id");
        tickets.reserveSeat(UUID.fromString(vip));
        tickets.flush();
        mvc.perform(post("/events/{id}/publish", eventId).with(user(organizer)));

        mvc.perform(get("/events/by-slug/{slug}", (String) JsonPath.read(body, "$.slug")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.organizerId").doesNotExist())
            .andExpect(jsonPath("$.venueBookingId").doesNotExist())
            .andExpect(jsonPath("$.tickets.length()").value(2))
            .andExpect(jsonPath("$.tickets[1].name").value("VIP"))
            .andExpect(jsonPath("$.tickets[1].soldOut").value(true))
            .andExpect(jsonPath("$.tickets[1].sold").doesNotExist());
    }

    // --- yardımcılar ---

    private String createAddressEvent() throws Exception {
        return createAddressEventAs(organizer);
    }

    private String createAddressEventAs(UUID userId) throws Exception {
        return JsonPath.read(create(userId, addressEventJson()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String createVenueEvent(int venueCapacity) throws Exception {
        UUID bookingId = UUID.randomUUID();
        given(venueBookings.findConfirmed(bookingId, organizer)).willReturn(Optional.of(new ConfirmedVenueBooking(
            bookingId, UUID.randomUUID(), "Sahne", venueCapacity, start, start.plus(Duration.ofHours(3)))));
        return JsonPath.read(create(organizer, """
                {"title": "Sahne Gecesi", "visibility": "PUBLIC", "venueBookingId": "%s"}
                """.formatted(bookingId)).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String addressEventJson() {
        return """
                {"title": "Kitap Kulübü", "visibility": "PUBLIC", "customAddress": "Moda Parkı",
                 "startsAt": "%s", "endsAt": "%s"}
                """.formatted(start.atOffset(ZoneOffset.ofHours(3)), start.plus(Duration.ofHours(2)).atOffset(ZoneOffset.ofHours(3)));
    }

    private ResultActions create(UUID userId, String json) throws Exception {
        return mvc.perform(post("/events").with(user(userId)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions listTickets(String eventId) throws Exception {
        return mvc.perform(get("/events/{id}/ticket-types", eventId).with(user(organizer)));
    }

    private String firstTicketId(String eventId) throws Exception {
        return JsonPath.read(listTickets(eventId).andReturn().getResponse().getContentAsString(), "$[0].id");
    }

    private ResultActions addTicket(String eventId, String name, String price, Integer capacity) throws Exception {
        return mvc.perform(post("/events/{id}/ticket-types", eventId).with(user(organizer))
            .contentType(MediaType.APPLICATION_JSON).content(ticketJson(name, price, capacity)));
    }

    private ResultActions addTicketAs(UUID userId, String eventId) throws Exception {
        return mvc.perform(post("/events/{id}/ticket-types", eventId).with(user(userId))
            .contentType(MediaType.APPLICATION_JSON).content(ticketJson("X", "0", null)));
    }

    private ResultActions updateTicket(String eventId, String ticketId, String name, String price, Integer capacity)
        throws Exception {
        return mvc.perform(put("/events/{e}/ticket-types/{t}", eventId, ticketId).with(user(organizer))
            .contentType(MediaType.APPLICATION_JSON).content(ticketJson(name, price, capacity)));
    }

    private ResultActions deleteTicket(String eventId, String ticketId) throws Exception {
        return mvc.perform(delete("/events/{e}/ticket-types/{t}", eventId, ticketId).with(user(organizer)));
    }

    private static String ticketJson(String name, String price, Integer capacity) {
        return """
                {"name": "%s", "price": %s, "capacity": %s}
                """.formatted(name, price, capacity);
    }

    private static RequestPostProcessor user(UUID userId) {
        return jwt().jwt(j -> j.subject(userId.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER"));
    }
}
