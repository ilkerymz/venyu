package com.venyu.identity.internal.web;

import com.venyu.identity.internal.domain.Role;
import com.venyu.identity.internal.domain.User;
import com.venyu.identity.internal.service.EmailVerificationService;
import com.venyu.identity.internal.service.RoleService;
import com.venyu.identity.internal.service.UserProfileService;
import com.venyu.identity.internal.web.dto.MeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
class MeController {

    private final UserProfileService userProfileService;
    private final RoleService roleService;
    private final EmailVerificationService emailVerificationService;

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

    @PostMapping("/me/roles/organizer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void becomeOrganizer(@AuthenticationPrincipal Jwt jwt) {
        roleService.becomeOrganizer(UUID.fromString(jwt.getSubject()));
    }

    // 202: e-posta outbox üzerinden, commit sonrası gider.
    @PostMapping("/me/email-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void resendEmailVerification(@AuthenticationPrincipal Jwt jwt) {
        emailVerificationService.resendVerification(UUID.fromString(jwt.getSubject()));
    }
}
