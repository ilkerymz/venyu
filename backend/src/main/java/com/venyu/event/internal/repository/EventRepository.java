package com.venyu.event.internal.repository;

import com.venyu.event.internal.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Optional<Event> findBySlug(String slug);

    List<Event> findByOrganizerIdOrderByStartsAtDesc(UUID organizerId);
}
