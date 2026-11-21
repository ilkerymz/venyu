package com.venyu.event.internal.web.dto;

import com.venyu.event.internal.domain.Event;
import com.venyu.event.internal.domain.TicketType;
import com.venyu.event.internal.service.EventService.EventWithTickets;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Ziyaretçiye açık görünüm: iç kimlikler (organizerId, venueBookingId) ve satış sayıları yok.
public record PublicEventResponse(
    String slug,
    String title,
    String description,
    boolean requiresAccount,
    UUID venueId,
    String customAddress,
    Instant startsAt,
    Instant endsAt,
    List<PublicTicket> tickets) {

    public static PublicEventResponse from(EventWithTickets view) {
        Event event = view.event();
        return new PublicEventResponse(event.getSlug(), event.getTitle(), event.getDescription(),
            event.isRequiresAccount(), event.getVenueId(), event.getCustomAddress(), event.getStartsAt(),
            event.getEndsAt(), view.tickets().stream().map(PublicTicket::from).toList());
    }

    public record PublicTicket(UUID id, String name, String description, BigDecimal price, String currency,
                               boolean soldOut) {

        static PublicTicket from(TicketType ticket) {
            Integer remaining = ticket.remaining();
            return new PublicTicket(ticket.getId(), ticket.getName(), ticket.getDescription(), ticket.getPrice(),
                ticket.getCurrency(), remaining != null && remaining == 0);
        }
    }
}
