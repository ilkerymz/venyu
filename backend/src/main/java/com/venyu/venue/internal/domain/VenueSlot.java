package com.venyu.venue.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "venue_slots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VenueSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID venueId;

    @Column(nullable = false, updatable = false)
    private Instant startsAt;

    @Column(nullable = false, updatable = false)
    private Instant endsAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static VenueSlot open(UUID venueId, Instant startsAt, Instant endsAt) {
        VenueSlot slot = new VenueSlot();
        slot.venueId = venueId;
        slot.startsAt = startsAt;
        slot.endsAt = endsAt;
        slot.createdAt = Instant.now();
        return slot;
    }
}