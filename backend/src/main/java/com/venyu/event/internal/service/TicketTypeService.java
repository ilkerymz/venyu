package com.venyu.event.internal.service;

import com.venyu.event.internal.domain.Event;
import com.venyu.event.internal.domain.TicketDetails;
import com.venyu.event.internal.domain.TicketType;
import com.venyu.event.internal.exception.EventNotFoundException;
import com.venyu.event.internal.exception.InvalidTicketException;
import com.venyu.event.internal.exception.NotEventOrganizerException;
import com.venyu.event.internal.exception.TicketTypeInUseException;
import com.venyu.event.internal.exception.TicketTypeNotFoundException;
import com.venyu.event.internal.repository.EventRepository;
import com.venyu.event.internal.repository.TicketTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketTypeService {

    private final TicketTypeRepository tickets;
    private final EventRepository events;
    private final EventService eventService;

    @Transactional
    public TicketType add(UUID userId, UUID eventId, TicketDetails details) {
        Event event = lockEditableEvent(userId, eventId);
        requireCapacityFits(event, details.capacity(), totalCapacityExcept(eventId, null));
        return tickets.saveAndFlush(TicketType.define(eventId, details));
    }

    @Transactional
    public TicketType update(UUID userId, UUID eventId, UUID ticketTypeId, TicketDetails details) {
        Event event = lockEditableEvent(userId, eventId);
        TicketType ticket = tickets.findByIdAndEventId(ticketTypeId, eventId)
            .orElseThrow(TicketTypeNotFoundException::new);
        requireCapacityFits(event, details.capacity(), totalCapacityExcept(eventId, ticketTypeId));
        ticket.update(details, Instant.now());
        try {
            tickets.flush();
        } catch (DataIntegrityViolationException e) {
            // ticket_types_sold_chk: biz kontrol ederken eşzamanlı satışlar kapasitenin üstüne çıktı.
            throw new InvalidTicketException("Kapasite, eşzamanlı satışlar nedeniyle satılanın altına düşüyor.");
        }
        return ticket;
    }

    @Transactional
    public void delete(UUID userId, UUID eventId, UUID ticketTypeId) {
        lockEditableEvent(userId, eventId);
        TicketType ticket = tickets.findByIdAndEventId(ticketTypeId, eventId)
            .orElseThrow(TicketTypeNotFoundException::new);
        if (ticket.hasSales()) {
            throw new TicketTypeInUseException("Satışı olan bilet türü silinemez.");
        }
        if (tickets.findByEventIdOrderByCreatedAtAsc(eventId).size() == 1) {
            throw new TicketTypeInUseException("Etkinliğin en az bir bilet türü olmalı.");
        }
        tickets.delete(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketType> listForOrganizer(UUID userId, UUID eventId) {
        eventService.getOwnedEvent(userId, eventId);
        return tickets.findByEventIdOrderByCreatedAtAsc(eventId);
    }

    private Event lockEditableEvent(UUID userId, UUID eventId) {
        Event event = events.findByIdForUpdate(eventId).orElseThrow(EventNotFoundException::new);
        if (!event.isOrganizedBy(userId)) {
            throw new NotEventOrganizerException();
        }
        event.requireTicketsEditable();
        return event;
    }

    private int totalCapacityExcept(UUID eventId, UUID excludedTicketId) {
        return tickets.findByEventIdOrderByCreatedAtAsc(eventId).stream()
            .filter(t -> !t.getId().equals(excludedTicketId))
            .map(TicketType::getCapacity)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum();
    }

    // Mekanlı etkinlik: kapasite zorunlu ve toplam mekan kapasitesini aşamaz. Adresli: serbest (null = sınırsız).
    private static void requireCapacityFits(Event event, Integer capacity, int otherTotal) {
        Integer limit = event.getCapacityLimit();
        if (limit == null) {
            return;
        }
        if (capacity == null) {
            throw new InvalidTicketException("Mekandaki etkinlikte bilet kapasitesi sınırsız olamaz.");
        }
        if (otherTotal + capacity > limit) {
            throw new InvalidTicketException(
                "Toplam bilet kapasitesi mekan kapasitesini (" + limit + ") aşamaz.");
        }
    }
}
