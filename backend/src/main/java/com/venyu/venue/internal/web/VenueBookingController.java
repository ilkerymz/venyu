package com.venyu.venue.internal.web;

import com.venyu.venue.internal.service.VenueBookingService;
import com.venyu.venue.internal.web.dto.BookingResponse;
import com.venyu.venue.internal.web.dto.HoldRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
class VenueBookingController {

    private final VenueBookingService bookingService;

    @PostMapping("/venues/{venueId}/slots/{slotId}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ORGANIZER')")
    BookingResponse hold(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId, @PathVariable UUID slotId,
                         @Valid @RequestBody HoldRequest request) {
        return BookingResponse.from(bookingService.hold(userId(jwt), venueId, slotId, request.note()));
    }

    @PostMapping("/bookings/{bookingId}/confirm")
    @PreAuthorize("hasRole('VENUE_OWNER')")
    BookingResponse confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId) {
        return BookingResponse.from(bookingService.confirm(userId(jwt), bookingId));
    }

    @PostMapping("/bookings/{bookingId}/reject")
    @PreAuthorize("hasRole('VENUE_OWNER')")
    BookingResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId) {
        return BookingResponse.from(bookingService.reject(userId(jwt), bookingId));
    }

    @GetMapping("/venues/{venueId}/bookings")
    @PreAuthorize("hasRole('VENUE_OWNER')")
    List<BookingResponse> listForVenue(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
        return bookingService.listForVenue(userId(jwt), venueId).stream().map(BookingResponse::from).toList();
    }

    @GetMapping("/me/bookings")
    @PreAuthorize("hasRole('ORGANIZER')")
    List<BookingResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return bookingService.listForOrganizer(userId(jwt)).stream().map(BookingResponse::from).toList();
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}