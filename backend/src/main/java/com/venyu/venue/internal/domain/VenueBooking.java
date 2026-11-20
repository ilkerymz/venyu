package com.venyu.venue.internal.domain;

import com.venyu.venue.internal.exception.BookingStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "venue_bookings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VenueBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID slotId;

    @Column(nullable = false, updatable = false)
    private UUID venueId;

    @Column(nullable = false, updatable = false)
    private UUID organizerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    private String note;

    @Column(nullable = false, updatable = false)
    private Instant expiresAt;

    // Optimistic locking: her güncellemede artar; arada başkası değiştirdiyse kaydetme başarısız olur.
    @Version
    private long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public static VenueBooking hold(VenueSlot slot, UUID organizerId, String note, Instant expiresAt) {
        VenueBooking booking = new VenueBooking();
        booking.slotId = slot.getId();
        booking.venueId = slot.getVenueId();
        booking.organizerId = organizerId;
        booking.note = note;
        booking.status = BookingStatus.HELD;
        booking.expiresAt = expiresAt;
        booking.createdAt = Instant.now();
        booking.updatedAt = booking.createdAt;
        return booking;
    }

    public void confirm(Instant now) {
        requireActiveHold(now);
        transitionTo(BookingStatus.CONFIRMED, now);
    }

    public void reject(Instant now) {
        requireActiveHold(now);
        transitionTo(BookingStatus.REJECTED, now);
    }

    private void requireActiveHold(Instant now) {
        if (status != BookingStatus.HELD) {
            throw new BookingStateException("Sadece bekleyen (HELD) rezervasyonlar onaylanabilir veya reddedilebilir.");
        }
        // Job henüz EXPIRED'a çekmemiş olabilir: süre dolduysa geçişe izin verme.
        if (!expiresAt.isAfter(now)) {
            throw new BookingStateException("Rezervasyonun bekleme süresi dolmuş.");
        }
    }

    private void transitionTo(BookingStatus next, Instant now) {
        this.status = next;
        this.updatedAt = now;
    }
}