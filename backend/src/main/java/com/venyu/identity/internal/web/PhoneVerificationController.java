package com.venyu.identity.internal.web;

import com.venyu.identity.internal.service.PhoneVerificationService;
import com.venyu.identity.internal.web.dto.PhoneCodeConfirmRequest;
import com.venyu.identity.internal.web.dto.PhoneCodeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/me/phone/verification")
@RequiredArgsConstructor
class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    void requestCode(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PhoneCodeRequest request) {
        phoneVerificationService.requestCode(UUID.fromString(jwt.getSubject()), request.phone());
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void confirmCode(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PhoneCodeConfirmRequest request) {
        phoneVerificationService.confirmCode(UUID.fromString(jwt.getSubject()), request.code());
    }
}