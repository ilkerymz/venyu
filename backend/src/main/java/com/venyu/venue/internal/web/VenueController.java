package com.venyu.venue.internal.web;

import com.venyu.venue.internal.service.VenueService;
import com.venyu.venue.internal.web.dto.VenueRequest;
import com.venyu.venue.internal.web.dto.VenueResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/venues")
@RequiredArgsConstructor
class VenueController {

    private final VenueService venueService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VENUE_OWNER')")
    VenueResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody VenueRequest request) {
        return VenueResponse.from(venueService.create(userId(jwt), request.toDetails()));
    }

    @PutMapping("/{venueId}")
    @PreAuthorize("hasRole('VENUE_OWNER')")
    VenueResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
                         @Valid @RequestBody VenueRequest request) {
        return VenueResponse.from(venueService.update(userId(jwt), venueId, request.toDetails()));
    }

    @GetMapping
    List<VenueResponse> list(@RequestParam(required = false) String district) {
        return venueService.list(district).stream().map(VenueResponse::from).toList();
    }

    @GetMapping("/{venueId}")
    VenueResponse get(@PathVariable UUID venueId) {
        return VenueResponse.from(venueService.get(venueId));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}