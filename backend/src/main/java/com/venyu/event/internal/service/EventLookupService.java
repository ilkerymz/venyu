package com.venyu.event.internal.service;

import com.venyu.event.EventLookup;
import com.venyu.event.EventSummary;
import com.venyu.event.TicketOffer;
import com.venyu.event.internal.domain.TicketType;
import com.venyu.event.internal.repository.EventRepository;
import com.venyu.event.internal.repository.TicketTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class EventLookupService implements EventLookup {

    private final EventRepository events;
    private final TicketTypeRepository tickets;

    @Override
    @Transactional(readOnly = true)
    public Optional<EventSummary> find(UUID eventId) {
        return events.findById(eventId).map(event -> new EventSummary(event.getId(), event.getTitle(),
            event.getSlug(), event.getOrganizerId(), event.getStartsAt(), event.getEndsAt(),
            event.isRequiresAccount()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketOffer> findTicketOffers(UUID eventId) {
        return tickets.findByEventIdOrderByCreatedAtAsc(eventId).stream().map(EventLookupService::toOffer).toList();
    }

    private static TicketOffer toOffer(TicketType ticket) {
        Integer remaining = ticket.remaining();
        return new TicketOffer(ticket.getId(), ticket.getName(), ticket.getDescription(), ticket.getPrice(),
            ticket.getCurrency(), remaining != null && remaining == 0);
    }
}
