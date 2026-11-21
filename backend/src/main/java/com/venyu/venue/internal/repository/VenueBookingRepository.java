package com.venyu.venue.internal.repository;

import com.venyu.venue.internal.domain.BookingStatus;
import com.venyu.venue.internal.domain.VenueBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
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

    // Toplu güncelleme @Version'ı atlar: versiyonu elle artırıyoruz ki
    // aynı anda bu rezervasyonu okumuş biri (örn. onaylayan sahip) optimistic lock'a takılsın.
    @Modifying
    @Query("""
            update VenueBooking b
            set b.status = :expired, b.version = b.version + 1, b.updatedAt = :now
            where b.status = :held and b.expiresAt <= :now
            """)
    int expireHolds(Instant now, BookingStatus held, BookingStatus expired);
}