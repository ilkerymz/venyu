package com.venyu.event;

import java.time.Instant;
import java.util.UUID;

/**
 * TicketInventory.reserve sonucu (ADR-007). Çağıran her durumu ele almak zorunda (sealed + switch).
 */
public sealed interface TicketReservation {

    record Reserved(UUID eventId, UUID ticketTypeId, Instant eventStartsAt) implements TicketReservation {
    }

    record SoldOut() implements TicketReservation {
    }

    record NotAvailable(String reason) implements TicketReservation {
    }

    record NotFound() implements TicketReservation {
    }
}
