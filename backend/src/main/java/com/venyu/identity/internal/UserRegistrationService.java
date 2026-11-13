package com.venyu.identity.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
class UserRegistrationService {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    User register(String email, String rawPassword, String displayName) {
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST,
                    ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Şifre çok uzun."), null);
        }
        if (users.existsByEmail(User.normalizeEmail(email))) {
            throw new EmailAlreadyRegisteredException();
        }
        try {
            return users.saveAndFlush(User.register(email, passwordEncoder.encode(rawPassword), displayName));
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyRegisteredException();
        }
    }
}
