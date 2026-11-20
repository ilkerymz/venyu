package com.venyu.identity.internal.web;

import com.venyu.identity.internal.domain.User;
import com.venyu.identity.internal.service.AuthenticationService;
import com.venyu.identity.internal.service.EmailVerificationService;
import com.venyu.identity.internal.service.TokenService;
import com.venyu.identity.internal.service.UserRegistrationService;
import com.venyu.identity.internal.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
class AuthController {

    private final UserRegistrationService registrationService;
    private final AuthenticationService authenticationService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = registrationService.register(request.email(), request.password(), request.displayName());
        return new RegisterResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return toResponse(authenticationService.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return toResponse(authenticationService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody RefreshRequest request) {
        authenticationService.logout(request.refreshToken());
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verify(request.token());
    }

    private static TokenResponse toResponse(TokenService.IssuedTokens tokens) {
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken(), tokens.expiresInSeconds());
    }

}
