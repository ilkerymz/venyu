package com.venyu.event.internal.web.dto;

import com.venyu.event.internal.domain.TicketType;

import java.math.BigDecimal;
import java.util.UUID;

public record TicketTypeResponse(
    UUID id,
    String name,
    String description,
    BigDecimal price,
    String currency,
    Integer capacity,
    int sold,
    Integer remaining) {

    public static TicketTypeResponse from(TicketType ticket) {
        return new TicketTypeResponse(ticket.getId(), ticket.getName(), ticket.getDescription(), ticket.getPrice(),
            ticket.getCurrency(), ticket.getCapacity(), ticket.getSold(), ticket.remaining());
    }
}
