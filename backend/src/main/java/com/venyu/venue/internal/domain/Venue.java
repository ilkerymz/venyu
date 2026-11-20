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
@Table(name = "venues")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public static Venue create(UUID ownerId, VenueDetails details) {
        Venue venue = new Venue();
        venue.ownerId = ownerId;
        venue.createdAt = Instant.now();
        venue.apply(details);
        return venue;
    }

    public void update(VenueDetails details) {
        apply(details);
    }

    public boolean isOwnedBy(UUID userId) {
        return ownerId.equals(userId);
    }

    private void apply(VenueDetails details) {
        this.name = details.name();
        this.description = details.description();
        this.address = details.address();
        this.district = details.district();
        this.city = details.city();
        this.capacity = details.capacity();
        this.updatedAt = Instant.now();
    }
}