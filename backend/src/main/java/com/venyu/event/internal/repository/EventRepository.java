package com.venyu.event.internal.repository;

import com.venyu.event.internal.domain.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Optional<Event> findBySlug(String slug);

    List<Event> findByOrganizerIdOrderByStartsAtDesc(UUID organizerId);

    // Bilet türü işlemleri sırasında etkinliği kilitler: toplam kapasite kontrolü + ekleme atomik olsun.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForUpdate(UUID id);

    // Koltuk ayırırken (SELECT ... FOR SHARE): eşzamanlı kayıtlar birbirini beklemez, ama etkinliği değiştiren
    // UPDATE (örn. iptal) devam eden kayıtların commit olmasını bekler. İptalden sonra gelen kayıt CANCELLED görür.
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForShare(UUID id);
}
