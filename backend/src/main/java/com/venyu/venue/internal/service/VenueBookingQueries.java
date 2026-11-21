package com.venyu.venue.internal.service;

import com.venyu.venue.ConfirmedVenueBooking;
import com.venyu.venue.VenueBookings;
import com.venyu.venue.internal.domain.BookingStatus;
import com.venyu.venue.internal.domain.Venue;
import com.venyu.venue.internal.domain.VenueSlot;
import com.venyu.venue.internal.repository.VenueBookingRepository;
import com.venyu.venue.internal.repository.VenueRepository;
import com.venyu.venue.internal.repository.VenueSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class VenueBookingQueries implements VenueBookings {

    private final VenueBookingRepository bookings;
    private final VenueSlotRepository slots;
    private final VenueRepository venues;

    @Override
    @Transactional(readOnly = true)
    public Optional<ConfirmedVenueBooking> findConfirmed(UUID bookingId, UUID organizerId) {
        return bookings.findById(bookingId)
            .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
            .filter(booking -> booking.getOrganizerId().equals(organizerId))
            .map(booking -> {
                VenueSlot slot = slots.findById(booking.getSlotId()).orElseThrow();
                Venue venue = venues.findById(booking.getVenueId()).orElseThrow();
                return new ConfirmedVenueBooking(booking.getId(), venue.getId(), venue.getName(),
                    venue.getCapacity(), slot.getStartsAt(), slot.getEndsAt());
            });
    }
}
