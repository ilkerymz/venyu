package com.venyu.registration.internal.service;

import com.venyu.event.EventLookup;
import com.venyu.event.EventSummary;
import com.venyu.registration.internal.exception.EventOrganizerOnlyException;
import com.venyu.registration.internal.exception.RegistrationTargetNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

// Organizatöre ait işlemler (davet, katılımcı listesi, check-in) için ortak sahiplik kontrolü.
@Component
@RequiredArgsConstructor
class OrganizerEvents {

    private final EventLookup eventLookup;

    EventSummary require(UUID organizerId, UUID eventId) {
        EventSummary event = eventLookup.find(eventId).orElseThrow(RegistrationTargetNotFoundException::new);
        if (!event.isOrganizedBy(organizerId)) {
            throw new EventOrganizerOnlyException();
        }
        return event;
    }
}
