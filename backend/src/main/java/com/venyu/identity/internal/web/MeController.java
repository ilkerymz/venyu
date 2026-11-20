package com.venyu.identity.internal.web;

import com.venyu.identity.internal.domain.Role;
import com.venyu.identity.internal.domain.User;
import com.venyu.identity.internal.service.UserProfileService;
import com.venyu.identity.internal.web.dto.MeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
class MeController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        User user = userProfileService.getProfile(UUID.fromString(jwt.getSubject()));
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRoles().stream().map(Role::name).sorted().toList(),
                user.isEmailVerified(),
                user.getPhone(),
                user.isPhoneVerified());
    }
}