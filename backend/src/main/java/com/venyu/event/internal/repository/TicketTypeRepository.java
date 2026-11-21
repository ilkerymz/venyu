package com.venyu.event.internal.repository;

import com.venyu.event.internal.domain.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {

    List<TicketType> findByEventIdOrderByCreatedAtAsc(UUID eventId);

    // Bilet etkinliğiyle birlikte aranır: başka etkinliğin bileti, kendi etkinliğinin URL'i üzerinden değiştirilemesin.
    Optional<TicketType> findByIdAndEventId(UUID id, UUID eventId);

    // flushAutomatically: bekleyen değişiklikler önce DB'ye yazılsın (clear'da kaybolmasın).
    // clearAutomatically: bulk update sonrası persistence context temizlensin; sonraki okumalar DB'den taze gelsin.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TicketType t set t.sold = t.sold + 1 where t.id = :id and (t.capacity is null or t.sold < t.capacity)")
    int reserveSeat(UUID id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TicketType t set t.sold = t.sold - 1 where t.id = :id and t.sold > 0")
    int releaseSeat(UUID id);
}
