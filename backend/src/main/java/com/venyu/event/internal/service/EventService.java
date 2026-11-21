package com.venyu.event.internal.service;

import com.venyu.event.internal.domain.Event;
import com.venyu.event.internal.domain.EventDetails;
import com.venyu.event.internal.domain.EventLocation;
import com.venyu.event.internal.exception.EventConflictException;
import com.venyu.event.internal.exception.EventNotFoundException;
import com.venyu.event.internal.exception.InvalidEventException;
import com.venyu.event.internal.exception.NotEventOrganizerException;
import com.venyu.event.internal.exception.VenueBookingAlreadyUsedException;
import com.venyu.event.internal.repository.EventRepository;
import com.venyu.venue.ConfirmedVenueBooking;
import com.venyu.venue.VenueBookings;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository events;
    private final VenueBookings venueBookings;

    @Transactional
    public Event create(UUID organizerId, EventDetails details, EventLocation location) {
        Event event = switch (location) {
            case EventLocation.AtVenue atVenue -> {
                ConfirmedVenueBooking booking = venueBookings.findConfirmed(atVenue.bookingId(), organizerId)
                    .orElseThrow(() -> new InvalidEventException(
                        "Mekan rezervasyonu bulunamadı, onaylanmamış veya sana ait değil."));
                requireFuture(booking.startsAt());
                yield Event.atVenue(organizerId, Slugs.generate(), details,
                    booking.bookingId(), booking.venueId(), booking.startsAt(), booking.endsAt());
            }
            case EventLocation.AtAddress atAddress -> {
                if (!atAddress.endsAt().isAfter(atAddress.startsAt())) {
                    throw new InvalidEventException("Bitiş zamanı başlangıçtan sonra olmalı.");
                }
                requireFuture(atAddress.startsAt());
                yield Event.atAddress(organizerId, Slugs.generate(), details,
                    atAddress.address(), atAddress.startsAt(), atAddress.endsAt());
            }
        };
        try {
            return events.saveAndFlush(event);
        } catch (DataIntegrityViolationException e) {
            // UNIQUE (venue_booking_id). Slug çakışması da buraya düşerdi ama olasılığı ihmal edilebilir.
            throw new VenueBookingAlreadyUsedException();
        }
    }

    @Transactional
    public Event update(UUID userId, UUID eventId, EventDetails details) {
        return change(userId, eventId, (event, now) -> event.update(details, now));
    }

    @Transactional
    public Event publish(UUID userId, UUID eventId) {
        return change(userId, eventId, Event::publish);
    }

    @Transactional
    public Event cancel(UUID userId, UUID eventId) {
        return change(userId, eventId, Event::cancel);
    }

    @Transactional(readOnly = true)
    public Event getOwnedEvent(UUID userId, UUID eventId) {
        Event event = events.findById(eventId).orElseThrow(EventNotFoundException::new);
        if (!event.isOrganizedBy(userId)) {
            throw new NotEventOrganizerException();
        }
        return event;
    }

    @Transactional(readOnly = true)
    public List<Event> listForOrganizer(UUID organizerId) {
        return events.findByOrganizerIdOrderByStartsAtDesc(organizerId);
    }

    // viewerId null olabilir (anonim). Görünmeyen etkinlik için 404: varlığı da açığa vurulmaz.
    @Transactional(readOnly = true)
    public Event getBySlug(String slug, UUID viewerId) {
        Event event = events.findBySlug(slug).orElseThrow(EventNotFoundException::new);
        if (event.isVisibleToPublic() || (viewerId != null && event.isOrganizedBy(viewerId))) {
            return event;
        }
        throw new EventNotFoundException();
    }

    private Event change(UUID userId, UUID eventId, BiConsumer<Event, Instant> action) {
        Event event = getOwnedEvent(userId, eventId);
        action.accept(event, Instant.now());
        try {
            events.flush();
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new EventConflictException();
        }
        return event;
    }

    private static void requireFuture(Instant startsAt) {
        if (!startsAt.isAfter(Instant.now())) {
            throw new InvalidEventException("Geçmiş veya başlamış bir zamana etkinlik oluşturulamaz.");
        }
    }
}
