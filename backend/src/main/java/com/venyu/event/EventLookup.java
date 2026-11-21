package com.venyu.event;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Diğer modüllere açık, salt okunur etkinlik bilgisi (organizatör kontrolü, bildirim metinleri, davet önizlemesi).
 */
public interface EventLookup {

    Optional<EventSummary> find(UUID eventId);

    List<TicketOffer> findTicketOffers(UUID eventId);
}
