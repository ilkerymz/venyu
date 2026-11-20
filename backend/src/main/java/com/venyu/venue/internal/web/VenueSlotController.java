package com.venyu.venue.internal.web;

import com.venyu.venue.internal.service.VenueSlotService;
import com.venyu.venue.internal.web.dto.SlotRequest;
import com.venyu.venue.internal.web.dto.SlotResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/venues/{venueId}/slots")
@RequiredArgsConstructor
class VenueSlotController {

    private final VenueSlotService slotService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VENUE_OWNER')")
    SlotResponse open(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
                      @Valid @RequestBody SlotRequest request) {
        return SlotResponse.from(slotService.open(UUID.fromString(jwt.getSubject()), venueId,
                request.startsAt().toInstant(), request.endsAt().toInstant()));
    }

    @DeleteMapping("/{slotId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('VENUE_OWNER')")
    void close(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId, @PathVariable UUID slotId) {
        slotService.close(UUID.fromString(jwt.getSubject()), venueId, slotId);
    }

    @GetMapping
    List<SlotResponse> list(@PathVariable UUID venueId,
                            @RequestParam(required = false) Instant from,
                            @RequestParam(required = false) Instant to) {
        return slotService.list(venueId, from, to).stream().map(SlotResponse::from).toList();
    }
}