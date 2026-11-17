package com.venyu.identity.internal.service;

import com.venyu.identity.internal.domain.User;
import com.venyu.identity.internal.exception.InvalidCredentialsException;
import com.venyu.identity.internal.repository.UserRepository;
import com.venyu.identity.internal.service.TokenService.IssuedTokens;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class AuthenticationService {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final String dummyPasswordHash;

    public AuthenticationService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public IssuedTokens login(String email, String rawPassword) {
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new InvalidCredentialsException();
        }

        Optional<User> user = users.findByEmail(User.normalizeEmail(email));
        String hash = user.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(rawPassword, hash);

        if (user.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }
        return tokenService.issueTokens(user.get());
    }
}