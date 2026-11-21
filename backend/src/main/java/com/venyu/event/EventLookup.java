package com.venyu.event;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Diğer modüllere açık, salt okunur etkinlik bilgisi (organizatör kontrolü, bildirim metinleri, davet önizlemesi,
 * kullanıcının kayıtları).
 */
public interface EventLookup {

    Optional<EventSummary> find(UUID eventId);

    // Tek sorgu; bulunamayan id'ler sonuçta yer almaz.
    List<EventSummary> findAll(Collection<UUID> eventIds);

    List<TicketOffer> findTicketOffers(UUID eventId);
}
