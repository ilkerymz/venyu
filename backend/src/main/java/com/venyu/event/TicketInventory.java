package com.venyu.event;

import java.util.UUID;

/**
 * Diğer modüllere açık bilet envanteri API'si (ADR-007). Çağıranın transaction'ına katılır:
 * çağıranın işlemi geri alınırsa ayrılan koltuk da geri alınır.
 */
public interface TicketInventory {

    TicketReservation reserve(UUID eventId, UUID ticketTypeId);

    void release(UUID ticketTypeId);
}
