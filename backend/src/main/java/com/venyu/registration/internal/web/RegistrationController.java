package com.venyu.registration.internal.web;

import com.venyu.registration.internal.service.RegistrationService;
import com.venyu.registration.internal.web.dto.RegistrationRequest;
import com.venyu.registration.internal.web.dto.RegistrationResponse;
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
        return RegistrationResponse.from(registrationService.register(userId(jwt), eventId, request.ticketTypeId()));
    }

    @PostMapping("/registrations/{registrationId}/cancel")
    RegistrationResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID registrationId) {
        return RegistrationResponse.from(registrationService.cancel(userId(jwt), registrationId));
    }

    @GetMapping("/me/registrations")
    List<RegistrationResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return registrationService.listMine(userId(jwt)).stream().map(RegistrationResponse::from).toList();
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
