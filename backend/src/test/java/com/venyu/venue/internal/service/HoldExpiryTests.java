package com.venyu.venue.internal.service;

import com.venyu.TestcontainersConfiguration;
import com.venyu.venue.internal.domain.BookingStatus;
import com.venyu.venue.internal.domain.VenueBooking;
import com.venyu.venue.internal.domain.VenueDetails;
import com.venyu.venue.internal.domain.VenueSlot;
import com.venyu.venue.internal.repository.VenueBookingRepository;
import com.venyu.venue.internal.repository.VenueRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Job'un mantığı servis üzerinden doğrudan test edilir; zamanlayıcı testlerde kapalı (venyu.scheduling.enabled=false).
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class HoldExpiryTests {

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

    @BeforeEach
    void createVenue() {
        venueId = venueService.create(owner,
                new VenueDetails("Test Mekan", null, "Adres", "Kadıköy", "İstanbul", 80)).getId();
    }

    @AfterEach
    void cleanUp() {
        venues.deleteAll();
    }

    @Test
    void onlyExpiredHoldsAreExpired() {
        VenueBooking expired = expiredHold(openSlot(19));
        VenueBooking active = bookingService.hold(organizer, venueId, openSlot(12).getId(), null);
        VenueBooking confirmed = bookingService.hold(organizer, venueId, openSlot(15).getId(), null);
        bookingService.confirm(owner, confirmed.getId());

        assertThat(bookingService.expireHolds()).isEqualTo(1);

        assertThat(statusOf(expired)).isEqualTo(BookingStatus.EXPIRED);
        assertThat(statusOf(active)).isEqualTo(BookingStatus.HELD);
        assertThat(statusOf(confirmed)).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void expiringIsIdempotent() {
        expiredHold(openSlot(19));

        assertThat(bookingService.expireHolds()).isEqualTo(1);
        assertThat(bookingService.expireHolds()).isZero();
    }

    @Test
    void expiredSlotCanBeHeldAgain() {
        VenueSlot slot = openSlot(19);
        expiredHold(slot);
        bookingService.expireHolds();

        VenueBooking again = bookingService.hold(UUID.randomUUID(), venueId, slot.getId(), null);

        assertThat(again.getStatus()).isEqualTo(BookingStatus.HELD);
    }

    @Test
    void expiringIncrementsVersionSoStaleCopiesAreRejected() {
        VenueBooking expired = expiredHold(openSlot(19));
        VenueBooking stale = bookings.findById(expired.getId()).orElseThrow();

        bookingService.expireHolds();

        VenueBooking current = bookings.findById(expired.getId()).orElseThrow();
        assertThat(current.getVersion()).isEqualTo(stale.getVersion() + 1);
        // Job'dan önce okunmuş bir kopya artık yazılamaz: bulk update'teki "version + 1" olmasaydı yazılırdı.
        assertThatThrownBy(() -> bookings.saveAndFlush(stale))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    private VenueSlot openSlot(int hour) {
        Instant day = Instant.now().plus(Duration.ofDays(7)).truncatedTo(ChronoUnit.DAYS);
        Instant start = day.plus(Duration.ofHours(hour));
        return slotService.open(owner, venueId, start, start.plus(Duration.ofHours(2)));
    }

    private VenueBooking expiredHold(VenueSlot slot) {
        return bookings.saveAndFlush(
                VenueBooking.hold(slot, organizer, null, Instant.now().minus(Duration.ofMinutes(1))));
    }

    private BookingStatus statusOf(VenueBooking booking) {
        return bookings.findById(booking.getId()).orElseThrow().getStatus();
    }
}