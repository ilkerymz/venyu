package com.venyu.venue.internal.service;

import com.venyu.TestcontainersConfiguration;
import com.venyu.venue.ConfirmedVenueBooking;
import com.venyu.venue.VenueBookings;
import com.venyu.venue.internal.domain.VenueBooking;
import com.venyu.venue.internal.domain.VenueDetails;
import com.venyu.venue.internal.domain.VenueSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Diğer modüllere açılan sözleşmenin (ADR-007) venue tarafındaki testi.
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
@Transactional
class VenueBookingQueriesTests {

    @Autowired
    VenueBookings venueBookings;

    @Autowired
    VenueService venueService;

    @Autowired
    VenueSlotService slotService;

    @Autowired
    VenueBookingService bookingService;

    UUID owner = UUID.randomUUID();
    UUID organizer = UUID.randomUUID();
    UUID venueId;
    VenueSlot slot;

    @BeforeEach
    void createVenueAndSlot() {
        venueId = venueService.create(owner,
            new VenueDetails("Kadıköy Sahne", null, "Adres", "Kadıköy", "İstanbul", 80)).getId();
        Instant start = Instant.now().plus(Duration.ofDays(7)).truncatedTo(ChronoUnit.HOURS);
        slot = slotService.open(owner, venueId, start, start.plus(Duration.ofHours(3)));
    }

    @Test
    void returnsConfirmedBookingOfTheOrganizer() {
        VenueBooking booking = bookingService.hold(organizer, venueId, slot.getId(), null);
        bookingService.confirm(owner, booking.getId());

        ConfirmedVenueBooking result = venueBookings.findConfirmed(booking.getId(), organizer).orElseThrow();

        assertThat(result.venueId()).isEqualTo(venueId);
        assertThat(result.venueName()).isEqualTo("Kadıköy Sahne");
        assertThat(result.venueCapacity()).isEqualTo(80);
        assertThat(result.startsAt()).isEqualTo(slot.getStartsAt());
        assertThat(result.endsAt()).isEqualTo(slot.getEndsAt());
    }

    @Test
    void heldBookingIsNotReturned() {
        VenueBooking booking = bookingService.hold(organizer, venueId, slot.getId(), null);

        assertThat(venueBookings.findConfirmed(booking.getId(), organizer)).isEmpty();
    }

    @Test
    void someoneElsesBookingIsNotReturned() {
        VenueBooking booking = bookingService.hold(organizer, venueId, slot.getId(), null);
        bookingService.confirm(owner, booking.getId());

        assertThat(venueBookings.findConfirmed(booking.getId(), UUID.randomUUID())).isEmpty();
        assertThat(venueBookings.findConfirmed(UUID.randomUUID(), organizer)).isEmpty();
    }
}
