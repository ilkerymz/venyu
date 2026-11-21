package com.venyu.registration.internal.web;

import com.venyu.registration.internal.service.InvitationService;
import com.venyu.registration.internal.web.dto.AcceptInvitationAsGuestRequest;
import com.venyu.registration.internal.web.dto.AcceptInvitationRequest;
import com.venyu.registration.internal.web.dto.InvitationPreviewResponse;
import com.venyu.registration.internal.web.dto.InvitationRequest;
import com.venyu.registration.internal.web.dto.InvitationResponse;
import com.venyu.registration.internal.web.dto.InvitationTokenRequest;
import com.venyu.registration.internal.web.dto.RegistrationResponse;
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
class InvitationController {

    private final InvitationService invitationService;

    // Dönen liste sadece yeni oluşturulan davetler; daha önce davet edilmiş e-postalar atlanır.
    @PostMapping("/events/{eventId}/invitations")
    @PreAuthorize("hasRole('ORGANIZER')")
    @ResponseStatus(HttpStatus.CREATED)
    List<InvitationResponse> invite(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId,
                                    @Valid @RequestBody InvitationRequest request) {
        return invitationService.invite(userId(jwt), eventId, request.emails()).stream()
            .map(InvitationResponse::from).toList();
    }

    @GetMapping("/events/{eventId}/invitations")
    @PreAuthorize("hasRole('ORGANIZER')")
    List<InvitationResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId) {
        return invitationService.list(userId(jwt), eventId).stream().map(InvitationResponse::from).toList();
    }

    // POST: token URL'de (sunucu/proxy log'ları, Referer) görünmesin diye body'de.
    @PostMapping("/invitations/preview")
    InvitationPreviewResponse preview(@Valid @RequestBody InvitationTokenRequest request) {
        return InvitationPreviewResponse.from(invitationService.preview(request.token()));
    }

    @PostMapping("/invitations/accept")
    @ResponseStatus(HttpStatus.CREATED)
    RegistrationResponse accept(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AcceptInvitationRequest request) {
        return RegistrationResponse.from(
            invitationService.acceptAsMember(userId(jwt), request.token(), request.ticketTypeId()));
    }

    @PostMapping("/invitations/accept/guest")
    @ResponseStatus(HttpStatus.CREATED)
    RegistrationResponse acceptAsGuest(@Valid @RequestBody AcceptInvitationAsGuestRequest request) {
        return RegistrationResponse.from(
            invitationService.acceptAsGuest(request.token(), request.ticketTypeId(), request.name()));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
