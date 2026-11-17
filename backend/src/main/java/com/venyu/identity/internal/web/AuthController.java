package com.venyu.identity.internal.web;

import com.venyu.identity.internal.domain.User;
import com.venyu.identity.internal.service.AuthenticationService;
import com.venyu.identity.internal.service.TokenService;
import com.venyu.identity.internal.service.UserRegistrationService;
import com.venyu.identity.internal.web.dto.LoginRequest;
import com.venyu.identity.internal.web.dto.RegisterRequest;
import com.venyu.identity.internal.web.dto.RegisterResponse;
import com.venyu.identity.internal.web.dto.TokenResponse;
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

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = registrationService.register(request.email(), request.password(), request.displayName());
        return new RegisterResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        TokenService.IssuedTokens tokens = authenticationService.login(request.email(), request.password());
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken(), tokens.expiresInSeconds());
    }

}
