package com.venyu.venue.internal.web;

import com.jayway.jsonpath.JsonPath;
import com.venyu.TestcontainersConfiguration;
import com.venyu.venue.internal.domain.BookingStatus;
import com.venyu.venue.internal.domain.VenueBooking;
import com.venyu.venue.internal.domain.VenueDetails;
import com.venyu.venue.internal.domain.VenueSlot;
import com.venyu.venue.internal.exception.SlotAlreadyBookedException;
import com.venyu.venue.internal.repository.VenueBookingRepository;
import com.venyu.venue.internal.repository.VenueRepository;
import com.venyu.venue.internal.service.VenueBookingService;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Bilinçli olarak @Transactional değil: constraint ihlalleri ve eşzamanlılık gerçek commit'lerle test ediliyor.
@ApplicationModuleTest(extraIncludes = "identity")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VenueBookingTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    VenueService venueService;

    @Autowired
    VenueSlotService slotService;

    @Autowired
    VenueBookingService bookingService;

    @Autowired
    VenueRepository venues;

    @Autowired
    VenueBookingRepository bookings;

    UUID owner = UUID.randomUUID();
    UUID organizer = UUID.randomUUID();
    UUID venueId;
    VenueSlot slot;

    @BeforeEach
    void createVenueAndSlot() {
        venueId = venueService.create(owner,
                new VenueDetails("Test Mekan", null, "Adres", "Kadıköy", "İstanbul", 80)).getId();
        Instant start = Instant.now().plus(Duration.ofDays(7)).truncatedTo(ChronoUnit.HOURS);
        slot = slotService.open(owner, venueId, start, start.plus(Duration.ofHours(3)));
    }

    @AfterEach
    void cleanUp() {
        venues.deleteAll();
    }

    @Test
    void organizerHoldsAndOwnerConfirms() throws Exception {
        String bookingId = holdAndGetId(organizer);

        mvc.perform(get("/venues/{id}/slots", venueId))
                .andExpect(jsonPath("$[0].available").value(false));

        confirm(owner, bookingId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void onlyOrganizersCanHold() throws Exception {
        hold(user(UUID.randomUUID(), "PARTICIPANT")).andExpect(status().isForbidden());
    }

    @Test
    void activeSlotCannotBeHeldTwice() throws Exception {
        holdAndGetId(organizer);

        hold(user(UUID.randomUUID(), "ORGANIZER")).andExpect(status().isConflict());
    }

    @Test
    void rejectedSlotCanBeHeldAgain() throws Exception {
        String first = holdAndGetId(organizer);
        reject(owner, first).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));

        // Partial unique index: REJECTED satırı kural dışında, slot yeniden tutulabilir.
        hold(user(UUID.randomUUID(), "ORGANIZER")).andExpect(status().isCreated());
    }

    @Test
    void onlyHeldBookingsCanTransition() throws Exception {
        String bookingId = holdAndGetId(organizer);
        confirm(owner, bookingId).andExpect(status().isOk());

        confirm(owner, bookingId).andExpect(status().isConflict());
        reject(owner, bookingId).andExpect(status().isConflict());
    }

    @Test
    void expiredHoldCannotBeConfirmedEvenBeforeTheJobRuns() throws Exception {
        VenueBooking expired = bookings.saveAndFlush(
                VenueBooking.hold(slot, organizer, null, Instant.now().minus(Duration.ofMinutes(1))));

        confirm(owner, expired.getId().toString()).andExpect(status().isConflict());
    }

    @Test
    void ownerOfAnotherVenueCannotConfirmOrList() throws Exception {
        String bookingId = holdAndGetId(organizer);
        UUID otherOwner = UUID.randomUUID();

        confirm(otherOwner, bookingId).andExpect(status().isForbidden());
        mvc.perform(get("/venues/{id}/bookings", venueId).with(user(otherOwner, "VENUE_OWNER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/venues/{id}/bookings", venueId)).andExpect(status().isUnauthorized());

        mvc.perform(get("/venues/{id}/bookings", venueId).with(user(owner, "VENUE_OWNER")))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/me/bookings").with(user(organizer, "ORGANIZER")))
                .andExpect(jsonPath("$[0].id").value(bookingId));
    }

    @Test
    void slotWithBookingHistoryCannotBeClosed() throws Exception {
        holdAndGetId(organizer);

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/venues/{venueId}/slots/{slotId}", venueId, slot.getId())
                        .with(user(owner, "VENUE_OWNER")))
                .andExpect(status().isConflict());
    }

    @Test
    void concurrentHoldsOnSameSlotHaveExactlyOneWinner() throws Exception {
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            UUID someOrganizer = UUID.randomUUID();
            tasks.add(() -> {
                try {
                    bookingService.hold(someOrganizer, venueId, slot.getId(), null);
                    return true;
                } catch (SlotAlreadyBookedException e) {
                    return false;
                }
            });
        }

        assertThat(runConcurrently(tasks)).isEqualTo(1);
    }

    @Test
    void staleUpdateIsRejectedByOptimisticLocking() {
        VenueBooking booking = bookingService.hold(organizer, venueId, slot.getId(), null);
        VenueBooking stale = bookings.findById(booking.getId()).orElseThrow(); // version 0

        bookingService.confirm(owner, booking.getId()); // DB'de artık version 1

        stale.reject(Instant.now());
        assertThatThrownBy(() -> bookings.saveAndFlush(stale))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(bookings.findById(booking.getId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void concurrentConfirmAndRejectHaveExactlyOneWinner() throws Exception {
        UUID bookingId = bookingService.hold(organizer, venueId, slot.getId(), null).getId();

        List<Callable<Boolean>> tasks = List.of(
                () -> succeeds(() -> bookingService.confirm(owner, bookingId)),
                () -> succeeds(() -> bookingService.reject(owner, bookingId)));

        assertThat(runConcurrently(tasks)).isEqualTo(1);
        assertThat(bookings.findById(bookingId).orElseThrow().getStatus())
                .isIn(BookingStatus.CONFIRMED, BookingStatus.REJECTED);
    }

    // --- yardımcılar ---

    private static boolean succeeds(Runnable action) {
        try {
            action.run();
            return true;
        } catch (ErrorResponseException e) {
            // Kaybeden: BookingConflictException (optimistic lock) veya BookingStateException (zaten değişmiş).
            return false;
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
            if (result.get(10, TimeUnit.SECONDS)) {
                winners++;
            }
        }
        executor.shutdown();
        return winners;
    }

    private ResultActions hold(RequestPostProcessor user) throws Exception {
        return mvc.perform(post("/venues/{venueId}/slots/{slotId}/bookings", venueId, slot.getId()).with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"note": "Kitap kulübü"}
                        """));
    }

    private String holdAndGetId(UUID organizerId) throws Exception {
        return JsonPath.read(hold(user(organizerId, "ORGANIZER"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private ResultActions confirm(UUID ownerId, String bookingId) throws Exception {
        return mvc.perform(post("/bookings/{id}/confirm", bookingId).with(user(ownerId, "VENUE_OWNER")));
    }

    private ResultActions reject(UUID ownerId, String bookingId) throws Exception {
        return mvc.perform(post("/bookings/{id}/reject", bookingId).with(user(ownerId, "VENUE_OWNER")));
    }

    private static RequestPostProcessor user(UUID userId, String role) {
        return jwt().jwt(j -> j.subject(userId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}