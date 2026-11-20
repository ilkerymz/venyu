package com.venyu.venue.internal.repository;

import com.venyu.venue.internal.domain.BookingStatus;
import com.venyu.venue.internal.domain.VenueBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface VenueBookingRepository extends JpaRepository<VenueBooking, UUID> {

    List<VenueBooking> findByVenueIdOrderByCreatedAtDesc(UUID venueId);

    List<VenueBooking> findByOrganizerIdOrderByCreatedAtDesc(UUID organizerId);

    boolean existsBySlotId(UUID slotId);

    @Query("select b.slotId from VenueBooking b where b.venueId = :venueId and b.status in :statuses")
    Set<UUID> findSlotIdsByVenueIdAndStatusIn(UUID venueId, Collection<BookingStatus> statuses);
}