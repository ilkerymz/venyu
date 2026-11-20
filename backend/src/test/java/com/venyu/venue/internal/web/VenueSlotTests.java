package com.venyu.venue.internal.web;

import com.jayway.jsonpath.JsonPath;
import com.venyu.TestcontainersConfiguration;
import com.venyu.venue.internal.domain.VenueDetails;
import com.venyu.venue.internal.exception.SlotOverlapException;
import com.venyu.venue.internal.repository.VenueRepository;
import com.venyu.venue.internal.repository.VenueSlotRepository;
import com.venyu.venue.internal.service.VenueService;
import com.venyu.venue.internal.service.VenueSlotService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Bilinçli olarak @Transactional değil: constraint ihlali PostgreSQL transaction'ını "aborted" yapar
// ve aynı transaction'daki sonraki istekler patlar. Ayrıca eşzamanlılık testi commit gerektirir.
@ApplicationModuleTest(extraIncludes = "identity")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VenueSlotTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    VenueService venueService;

    @Autowired
    VenueSlotService slotService;

    @Autowired
    VenueRepository venues;

    @Autowired
    VenueSlotRepository slots;

    UUID owner = UUID.randomUUID();
    UUID venueId;
    // Testler takvime bağlı olmasın: her zaman 7 gün sonrası, saat başı.
    Instant day = Instant.now().plus(Duration.ofDays(7)).truncatedTo(ChronoUnit.DAYS);

    @BeforeEach
    void createVenue() {
        venueId = createVenueFor(owner);
    }

    @AfterEach
    void cleanUp() {
        venues.deleteAll();
    }

    @Test
    void ownerCanOpenSlotAndAnyoneCanListIt() throws Exception {
        open(owner, venueId, at(19), at(22)).andExpect(status().isCreated());

        mvc.perform(get("/venues/{id}/slots", venueId)
                        .param("from", day.toString())
                        .param("to", day.plus(Duration.ofDays(1)).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].startsAt").value(at(19).toString()));
    }

    @Test
    void overlappingSlotIsRejectedButBackToBackIsAllowed() throws Exception {
        open(owner, venueId, at(19), at(22)).andExpect(status().isCreated());

        open(owner, venueId, at(20), at(23)).andExpect(status().isConflict());
        open(owner, venueId, at(22), at(23)).andExpect(status().isCreated());
    }

    @Test
    void invalidTimesAreRejected() throws Exception {
        open(owner, venueId, at(22), at(19)).andExpect(status().isBadRequest());
        open(owner, venueId, Instant.now().minus(Duration.ofHours(2)), Instant.now().minus(Duration.ofHours(1)))
                .andExpect(status().isBadRequest());
        open(owner, venueId, at(10), at(10).plus(Duration.ofHours(25))).andExpect(status().isBadRequest());
    }

    @Test
    void onlyTheVenueOwnerCanOpenSlots() throws Exception {
        open(UUID.randomUUID(), venueId, at(19), at(22)).andExpect(status().isForbidden());
    }

    @Test
    void ownerCanCloseOwnSlot() throws Exception {
        String slotId = openAndGetId(owner, venueId, at(19), at(22));

        close(owner, venueId, slotId).andExpect(status().isNoContent());

        assertThat(slots.count()).isZero();
    }

    @Test
    void slotOfAnotherVenueCannotBeClosedThroughOwnVenue() throws Exception {
        String mySlot = openAndGetId(owner, venueId, at(19), at(22));
        UUID otherOwner = UUID.randomUUID();
        UUID otherVenue = createVenueFor(otherOwner);

        // IDOR #2: kendi mekanının sahibi, ama slot onun mekanına ait değil.
        close(otherOwner, otherVenue, mySlot).andExpect(status().isNotFound());

        assertThat(slots.findById(UUID.fromString(mySlot))).isPresent();
    }

    @Test
    void concurrentOverlappingSlotsHaveExactlyOneWinner() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int hour : new int[]{19, 20}) {
            results.add(executor.submit(() -> {
                start.await();
                try {
                    slotService.open(owner, venueId, at(hour), at(hour + 3));
                    return true;
                } catch (SlotOverlapException e) {
                    return false;
                }
            }));
        }
        start.countDown();

        int winners = 0;
        for (Future<Boolean> result : results) {
            if (result.get(10, TimeUnit.SECONDS)) {
                winners++;
            }
        }
        executor.shutdown();

        assertThat(winners).isEqualTo(1);
        assertThat(slots.count()).isEqualTo(1);
    }

    private Instant at(int hour) {
        return day.plus(Duration.ofHours(hour));
    }

    private UUID createVenueFor(UUID ownerId) {
        return venueService.create(ownerId,
                new VenueDetails("Test Mekan", null, "Adres", "Kadıköy", "İstanbul", 80)).getId();
    }

    private ResultActions open(UUID userId, UUID venue, Instant startsAt, Instant endsAt) throws Exception {
        return mvc.perform(post("/venues/{id}/slots", venue).with(venueOwner(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"startsAt": "%s", "endsAt": "%s"}
                        """.formatted(startsAt.atOffset(ZoneOffset.ofHours(3)), endsAt.atOffset(ZoneOffset.ofHours(3)))));
    }

    private String openAndGetId(UUID userId, UUID venue, Instant startsAt, Instant endsAt) throws Exception {
        return JsonPath.read(open(userId, venue, startsAt, endsAt)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private ResultActions close(UUID userId, UUID venue, String slotId) throws Exception {
        return mvc.perform(delete("/venues/{venueId}/slots/{slotId}", venue, slotId).with(venueOwner(userId)));
    }

    private static RequestPostProcessor venueOwner(UUID userId) {
        return jwt().jwt(j -> j.subject(userId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_VENUE_OWNER"));
    }
}