package com.venyu.event.internal.web;

import com.venyu.event.internal.service.TicketTypeService;
import com.venyu.event.internal.web.dto.TicketTypeRequest;
import com.venyu.event.internal.web.dto.TicketTypeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events/{eventId}/ticket-types")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORGANIZER')")
class TicketTypeController {

    private final TicketTypeService ticketTypeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TicketTypeResponse add(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId,
                           @Valid @RequestBody TicketTypeRequest request) {
        return TicketTypeResponse.from(ticketTypeService.add(userId(jwt), eventId, request.toDetails()));
    }

    @PutMapping("/{ticketTypeId}")
    TicketTypeResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId,
                              @PathVariable UUID ticketTypeId, @Valid @RequestBody TicketTypeRequest request) {
        return TicketTypeResponse.from(ticketTypeService.update(userId(jwt), eventId, ticketTypeId, request.toDetails()));
    }

    @DeleteMapping("/{ticketTypeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId, @PathVariable UUID ticketTypeId) {
        ticketTypeService.delete(userId(jwt), eventId, ticketTypeId);
    }

    @GetMapping
    List<TicketTypeResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId) {
        return ticketTypeService.listForOrganizer(userId(jwt), eventId).stream()
            .map(TicketTypeResponse::from).toList();
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
