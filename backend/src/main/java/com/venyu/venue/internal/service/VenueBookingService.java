package com.venyu.venue.internal.service;

import com.venyu.venue.internal.domain.VenueBooking;
import com.venyu.venue.internal.domain.VenueSlot;
import com.venyu.venue.internal.exception.BookingConflictException;
import com.venyu.venue.internal.exception.BookingNotFoundException;
import com.venyu.venue.internal.exception.InvalidSlotException;
import com.venyu.venue.internal.exception.SlotAlreadyBookedException;
import com.venyu.venue.internal.exception.SlotNotFoundException;
import com.venyu.venue.internal.repository.VenueBookingRepository;
import com.venyu.venue.internal.repository.VenueSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

@Service
@RequiredArgsConstructor
public class VenueBookingService {

    private static final Duration HOLD_TTL = Duration.ofHours(24);

    private final VenueBookingRepository bookings;
    private final VenueSlotRepository slots;
    private final VenueService venueService;

    @Transactional
    public VenueBooking hold(UUID organizerId, UUID venueId, UUID slotId, String note) {
        VenueSlot slot = slots.findByIdAndVenueId(slotId, venueId).orElseThrow(SlotNotFoundException::new);
        Instant now = Instant.now();
        if (!slot.getStartsAt().isAfter(now)) {
            throw new InvalidSlotException("Başlamış veya geçmiş bir slot tutulamaz.");
        }
        // Slot 24 saatten yakınsa, hold en geç slot başlarken düşer.
        Instant expiresAt = min(now.plus(HOLD_TTL), slot.getStartsAt());
        try {
            return bookings.saveAndFlush(VenueBooking.hold(slot, organizerId, note, expiresAt));
        } catch (DataIntegrityViolationException e) {
            // venue_bookings_one_active_per_slot: slot'ta zaten HELD veya CONFIRMED bir rezervasyon var.
            throw new SlotAlreadyBookedException();
        }
    }

    @Transactional
    public VenueBooking confirm(UUID ownerId, UUID bookingId) {
        return transition(ownerId, bookingId, VenueBooking::confirm);
    }

    @Transactional
    public VenueBooking reject(UUID ownerId, UUID bookingId) {
        return transition(ownerId, bookingId, VenueBooking::reject);
    }

    @Transactional(readOnly = true)
    public List<VenueBooking> listForVenue(UUID ownerId, UUID venueId) {
        venueService.getOwnedVenue(ownerId, venueId);
        return bookings.findByVenueIdOrderByCreatedAtDesc(venueId);
    }

    @Transactional(readOnly = true)
    public List<VenueBooking> listForOrganizer(UUID organizerId) {
        return bookings.findByOrganizerIdOrderByCreatedAtDesc(organizerId);
    }

    private VenueBooking transition(UUID ownerId, UUID bookingId, BiConsumer<VenueBooking, Instant> action) {
        VenueBooking booking = bookings.findById(bookingId).orElseThrow(BookingNotFoundException::new);
        venueService.getOwnedVenue(ownerId, booking.getVenueId());
        action.accept(booking, Instant.now());
        try {
            // Optimistic lock kontrolü burada: UPDATE ... WHERE id = ? AND version = ?
            bookings.flush();
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BookingConflictException();
        }
        return booking;
    }

    private static Instant min(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }
}