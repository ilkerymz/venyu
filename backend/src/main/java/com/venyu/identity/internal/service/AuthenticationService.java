package com.venyu.identity.internal.service;

import com.venyu.identity.internal.domain.RefreshToken;
import com.venyu.identity.internal.domain.User;
import com.venyu.identity.internal.exception.InvalidCredentialsException;
import com.venyu.identity.internal.exception.InvalidRefreshTokenException;
import com.venyu.identity.internal.repository.RefreshTokenRepository;
import com.venyu.identity.internal.repository.UserRepository;
import com.venyu.identity.internal.service.TokenService.IssuedTokens;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

@Service
@Slf4j
public class AuthenticationService {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokens;
    private final TokenService tokenService;
    private final String dummyPasswordHash;

    public AuthenticationService(UserRepository users, RefreshTokenRepository refreshTokens,
                                 PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokens = refreshTokens;
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

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public IssuedTokens refresh(String rawRefreshToken) {
        String tokenHash = TokenService.hash(rawRefreshToken);
        Instant now = Instant.now();

        if (refreshTokens.revokeIfActive(tokenHash, now) == 1) {
            RefreshToken token = refreshTokens.findByTokenHash(tokenHash).orElseThrow();
            User user = users.findById(token.getUserId()).orElseThrow(InvalidRefreshTokenException::new);
            return tokenService.issueTokens(user);
        }

        // Revoke edilmemiş olsaydı yukarıda kazanırdık: revoke edilmişse bu bir reuse.
        refreshTokens.findByTokenHash(tokenHash)
                .filter(token -> token.getRevokedAt() != null)
                .ifPresent(token -> {
                    log.warn("Refresh token reuse detected, revoking all sessions for user {}", token.getUserId());
                    refreshTokens.revokeAllForUser(token.getUserId(), now);
                });

        throw new InvalidRefreshTokenException();
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.revokeIfActive(TokenService.hash(rawRefreshToken), Instant.now());
    }
}