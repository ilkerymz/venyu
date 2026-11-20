package com.venyu.identity.internal.service;

import com.venyu.identity.PasswordResetRequested;
import com.venyu.identity.internal.domain.PasswordResetToken;
import com.venyu.identity.internal.domain.User;
import com.venyu.identity.internal.exception.InvalidPasswordResetTokenException;
import com.venyu.identity.internal.exception.PasswordTooLongException;
import com.venyu.identity.internal.repository.PasswordResetTokenRepository;
import com.venyu.identity.internal.repository.RefreshTokenRepository;
import com.venyu.identity.internal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private final PasswordResetTokenRepository tokens;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    // Kayıtlı olmayan e-posta için sessizce hiçbir şey yapmaz (user enumeration'a karşı).
    @Transactional
    public void requestReset(String email) {
        users.findByEmail(User.normalizeEmail(email)).ifPresent(user -> {
            Instant now = Instant.now();
            tokens.invalidateActiveForUser(user.getId(), now);

            String rawToken = SecureTokens.generate();
            tokens.save(PasswordResetToken.issue(user.getId(), SecureTokens.hash(rawToken), now.plus(TOKEN_TTL)));
            events.publishEvent(new PasswordResetRequested(
                    user.getId(), user.getEmail(), user.getDisplayName(), rawToken));
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        // Token'dan önce: geçersiz şifre token'ı harcamasın.
        if (PasswordRules.exceedsBcryptLimit(newPassword)) {
            throw new PasswordTooLongException();
        }

        String tokenHash = SecureTokens.hash(rawToken);
        Instant now = Instant.now();
        if (tokens.consumeIfValid(tokenHash, now) == 0) {
            throw new InvalidPasswordResetTokenException();
        }

        PasswordResetToken token = tokens.findByTokenHash(tokenHash).orElseThrow();
        User user = users.findById(token.getUserId()).orElseThrow(InvalidPasswordResetTokenException::new);
        user.changePassword(passwordEncoder.encode(newPassword));
        user.verifyEmail(now);
        refreshTokens.revokeAllForUser(user.getId(), now);
    }
}