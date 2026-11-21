package com.venyu.event.internal.service;

import com.venyu.event.ReservationContext;
import com.venyu.event.TicketInventory;
import com.venyu.event.TicketReservation;
import com.venyu.event.internal.domain.Event;
import com.venyu.event.internal.domain.EventStatus;
import com.venyu.event.internal.domain.EventVisibility;
import com.venyu.event.internal.domain.TicketType;
import com.venyu.event.internal.repository.EventRepository;
import com.venyu.event.internal.repository.TicketTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class TicketInventoryService implements TicketInventory {

    private final EventRepository events;
    private final TicketTypeRepository tickets;

    @Override
    @Transactional
    public TicketReservation reserve(UUID eventId, UUID ticketTypeId, ReservationContext context) {
        // FOR SHARE: kayıt ↔ iptal yarışı kapanır (iptal, bu transaction bitene kadar bekler; ADR-007).
        Optional<Event> found = events.findByIdForShare(eventId);
        if (found.isEmpty()) {
            return new TicketReservation.NotFound();
        }
        Event event = found.get();
        if (event.getStatus() != EventStatus.PUBLISHED) {
            return new TicketReservation.NotAvailable("Etkinlik kayda açık değil.");
        }
        if (event.getVisibility() == EventVisibility.INVITE_ONLY && !context.withInvitation()) {
            return new TicketReservation.NotAvailable("Bu etkinliğe sadece davetle katılınabilir.");
        }
        if (event.isRequiresAccount() && context.asGuest()) {
            return new TicketReservation.NotAvailable("Bu etkinliğe katılmak için hesap gerekli.");
        }
        if (!event.getStartsAt().isAfter(Instant.now())) {
            return new TicketReservation.NotAvailable("Etkinlik başladı veya sona erdi.");
        }
        Optional<TicketType> ticket = tickets.findByIdAndEventId(ticketTypeId, eventId);
        if (ticket.isEmpty()) {
            return new TicketReservation.NotFound();
        }
        if (!ticket.get().isFree()) {
            return new TicketReservation.NotAvailable("Ücretli bilet satışı henüz açık değil.");
        }
        // Son adım: overselling koruması (atomik, context 5.1). Sonrasında entity'ler detached (clearAutomatically).
        if (tickets.reserveSeat(ticketTypeId) == 0) {
            return new TicketReservation.SoldOut();
        }
        return new TicketReservation.Reserved(eventId, ticketTypeId, event.getStartsAt());
    }

    @Override
    @Transactional
    public void release(UUID ticketTypeId) {
        tickets.releaseSeat(ticketTypeId);
    }
}
