package com.venyu.registration.internal.web;

import com.venyu.registration.internal.service.AttendeeService;
import com.venyu.registration.internal.web.dto.AttendeeResponse;
import com.venyu.registration.internal.web.dto.CheckInRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// Organizatör uçları. Rol kontrolü burada, etkinliğin sahibi olma kontrolü serviste (OrganizerEvents).
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORGANIZER')")
class AttendeeController {

    private final AttendeeService attendeeService;

    @GetMapping("/events/{eventId}/registrations")
    List<AttendeeResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId) {
        return attendeeService.list(userId(jwt), eventId).stream().map(AttendeeResponse::from).toList();
    }

    @PostMapping("/events/{eventId}/check-ins")
    AttendeeResponse checkIn(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId,
                             @Valid @RequestBody CheckInRequest request) {
        return AttendeeResponse.from(attendeeService.checkIn(userId(jwt), eventId, request.ticketCode()));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
