package com.venyu.event.internal.domain;

import com.venyu.event.internal.exception.EventStateException;
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
@Table(name = "events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID organizerId;

    @Column(nullable = false, updatable = false)
    private String slug;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Column(updatable = false)
    private UUID venueBookingId;

    @Column(updatable = false)
    private UUID venueId;

    @Column(updatable = false)
    private String customAddress;

    @Column(nullable = false, updatable = false)
    private Instant startsAt;

    @Column(nullable = false, updatable = false)
    private Instant endsAt;

    // Mekanlı etkinlikte toplam bilet kapasitesi sınırı (mekan kapasitesinin kopyası); adresli etkinlikte null.
    @Column(updatable = false)
    private Integer capacityLimit;


    @Version
    private long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant publishedAt;

    private Instant cancelledAt;

    public static Event atVenue(UUID organizerId, String slug, EventDetails details, UUID venueBookingId,
                                UUID venueId, Instant startsAt, Instant endsAt, int venueCapacity) {
        Event event = draft(organizerId, slug, details, startsAt, endsAt);
        event.venueBookingId = venueBookingId;
        event.venueId = venueId;
        event.capacityLimit = venueCapacity;
        return event;
    }

    public void requireTicketsEditable() {
        if (status != EventStatus.DRAFT && status != EventStatus.PUBLISHED) {
            throw new EventStateException("Bu etkinliğin biletleri artık düzenlenemez.");
        }
    }

    public static Event atAddress(UUID organizerId, String slug, EventDetails details,
                                  String address, Instant startsAt, Instant endsAt) {
        Event event = draft(organizerId, slug, details, startsAt, endsAt);
        event.customAddress = address;
        return event;
    }

    private static Event draft(UUID organizerId, String slug, EventDetails details, Instant startsAt, Instant endsAt) {
        Event event = new Event();
        event.organizerId = organizerId;
        event.slug = slug;
        event.status = EventStatus.DRAFT;
        event.startsAt = startsAt;
        event.endsAt = endsAt;
        event.createdAt = Instant.now();
        event.apply(details, event.createdAt);
        return event;
    }

    public void update(EventDetails details, Instant now) {
        if (status != EventStatus.DRAFT && status != EventStatus.PUBLISHED) {
            throw new EventStateException("Sadece taslak veya yayındaki etkinlikler düzenlenebilir.");
        }
        apply(details, now);
    }

    public void publish(Instant now) {
        if (status != EventStatus.DRAFT) {
            throw new EventStateException("Sadece taslak etkinlikler yayınlanabilir.");
        }
        if (!startsAt.isAfter(now)) {
            throw new EventStateException("Başlamış veya geçmiş bir etkinlik yayınlanamaz.");
        }
        this.status = EventStatus.PUBLISHED;
        this.publishedAt = now;
        this.updatedAt = now;
    }

    public void cancel(Instant now) {
        if (status != EventStatus.DRAFT && status != EventStatus.PUBLISHED) {
            throw new EventStateException("Bu etkinlik iptal edilemez.");
        }
        this.status = EventStatus.CANCELLED;
        this.cancelledAt = now;
        this.updatedAt = now;
    }

    public boolean isOrganizedBy(UUID userId) {
        return organizerId.equals(userId);
    }

    // Organizatör dışındakiler: sadece yayındaki ve davete kapalı olmayan etkinlikler.
    public boolean isVisibleToPublic() {
        return status == EventStatus.PUBLISHED && visibility != EventVisibility.INVITE_ONLY;
    }

    private void apply(EventDetails details, Instant now) {
        this.title = details.title();
        this.description = details.description();
        this.visibility = details.visibility();
        this.updatedAt = now;
    }
}
