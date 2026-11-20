package com.venyu.venue.internal.repository;

import com.venyu.venue.internal.domain.VenueSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VenueSlotRepository extends JpaRepository<VenueSlot, UUID> {

    @Query("""
            select s from VenueSlot s
            where s.venueId = :venueId and s.startsAt >= :from and s.startsAt < :to
            order by s.startsAt
            """)
    List<VenueSlot> findInRange(UUID venueId, Instant from, Instant to);

    // Slot'u mekanıyla birlikte arar: başka mekanın slot'u, kendi mekanının URL'i üzerinden silinemesin.
    Optional<VenueSlot> findByIdAndVenueId(UUID id, UUID venueId);
}