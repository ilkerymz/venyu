package com.venyu.event.internal.web;

import com.venyu.event.internal.service.EventService;
import com.venyu.event.internal.web.dto.CreateEventRequest;
import com.venyu.event.internal.web.dto.EventResponse;
import com.venyu.event.internal.web.dto.UpdateEventRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
class EventController {

    private final EventService eventService;

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ORGANIZER')")
    EventResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateEventRequest request) {
        return EventResponse.from(eventService.create(userId(jwt), request.toDetails(), request.toLocation()));
    }

    @PutMapping("/events/{eventId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    EventResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId,
                         @Valid @RequestBody UpdateEventRequest request) {
        return EventResponse.from(eventService.update(userId(jwt), eventId, request.toDetails()));
    }

    @PostMapping("/events/{eventId}/publish")
    @PreAuthorize("hasRole('ORGANIZER')")
    EventResponse publish(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId) {
        return EventResponse.from(eventService.publish(userId(jwt), eventId));
    }

    @PostMapping("/events/{eventId}/cancel")
    @PreAuthorize("hasRole('ORGANIZER')")
    EventResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId) {
        return EventResponse.from(eventService.cancel(userId(jwt), eventId));
    }

    @GetMapping("/events/{eventId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    EventResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId) {
        return EventResponse.from(eventService.getOwnedEvent(userId(jwt), eventId));
    }

    @GetMapping("/me/events")
    @PreAuthorize("hasRole('ORGANIZER')")
    List<EventResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return eventService.listForOrganizer(userId(jwt)).stream().map(EventResponse::from).toList();
    }

    // Public: anonim de çağırabilir; token varsa organizatör kendi taslağını da görebilir.
    @GetMapping("/events/by-slug/{slug}")
    EventResponse getBySlug(@AuthenticationPrincipal Jwt jwt, @PathVariable String slug) {
        return EventResponse.from(eventService.getBySlug(slug, jwt == null ? null : userId(jwt)));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
