package com.venyu.registration.internal.web;

import com.venyu.registration.internal.service.RegistrationService;
import com.venyu.registration.internal.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
class RegistrationController {

    private final RegistrationService registrationService;

    // Her giriş yapmış kullanıcı kayıt olabilir (PARTICIPANT herkeste var); ayrı rol kontrolü yok.
    @PostMapping("/events/{eventId}/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    RegistrationResponse register(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId,
                                  @Valid @RequestBody RegistrationRequest request) {
        return RegistrationResponse.from(registrationService.register(userId(jwt), eventId, request.ticketTypeId(),
            request.answers()));
    }

    // Misafirin bilet e-postasındaki "yönet" link'i. POST: token body'de (log'lara ve Referer'a düşmesin).
    @PostMapping("/registrations/manage")
    ManagedRegistrationResponse manage(@Valid @RequestBody ManageTokenRequest request) {
        return ManagedRegistrationResponse.from(registrationService.manage(request.token()));
    }

    @PostMapping("/registrations/manage/cancel")
    ManagedRegistrationResponse cancelByManageToken(@Valid @RequestBody ManageTokenRequest request) {
        return ManagedRegistrationResponse.from(registrationService.cancelByManageToken(request.token()));
    }

    @PostMapping("/registrations/{registrationId}/cancel")
    RegistrationResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID registrationId) {
        return RegistrationResponse.from(registrationService.cancel(userId(jwt), registrationId));
    }

    @GetMapping("/me/registrations")
    List<RegistrationResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return registrationService.listMine(userId(jwt)).stream().map(RegistrationResponse::from).toList();
    }

    // Hesapsız kayıt: koltuk 30 dk tutulur, e-postadaki link'le onaylanınca CONFIRMED olur.
    @PostMapping("/events/{eventId}/registrations/guest")
    @ResponseStatus(HttpStatus.CREATED)
    RegistrationResponse registerGuest(@PathVariable UUID eventId, @Valid @RequestBody GuestRegistrationRequest request) {
        return RegistrationResponse.from(
            registrationService.registerGuest(eventId, request.ticketTypeId(), request.email(), request.name(),
                request.answers()));
    }

    @PostMapping("/registrations/confirm")
    RegistrationResponse confirm(@Valid @RequestBody ConfirmRegistrationRequest request) {
        return RegistrationResponse.from(registrationService.confirmGuest(request.token()));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
