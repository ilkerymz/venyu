package com.venyu.registration.internal.repository;

import com.venyu.registration.internal.domain.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByTokenHash(String tokenHash);

    List<Invitation> findByEventIdOrderByCreatedAtAsc(UUID eventId);

    @Query("select i.email from Invitation i where i.eventId = :eventId and i.email in :emails")
    List<String> findExistingEmails(UUID eventId, Collection<String> emails);
}
