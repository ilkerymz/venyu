package com.venyu.identity.internal.web;

import com.venyu.identity.internal.web.dto.MeResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
class MeController {

    @GetMapping("/me")
    MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return new MeResponse(UUID.fromString(jwt.getSubject()), jwt.getClaimAsStringList("roles"));
    }
}