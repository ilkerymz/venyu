package com.venyu.identity.internal.service;

import com.venyu.identity.EmailVerificationRequested;
import com.venyu.identity.internal.domain.EmailVerificationToken;
import com.venyu.identity.internal.domain.User;
import com.venyu.identity.internal.exception.EmailAlreadyVerifiedException;
import com.venyu.identity.internal.exception.InvalidVerificationTokenException;
import com.venyu.identity.internal.exception.TooManyVerificationEmailsException;
import com.venyu.identity.internal.exception.UserNotFoundException;
import com.venyu.identity.internal.repository.EmailVerificationTokenRepository;
import com.venyu.identity.internal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Duration TOKEN_TTL = Duration.ofHours(24);
    private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);

    private final EmailVerificationTokenRepository tokens;
    private final UserRepository users;
    private final ApplicationEventPublisher events;

    @Transactional
    public void requestVerification(User user) {
        String rawToken = SecureTokens.generate();
        tokens.save(EmailVerificationToken.issue(
            user.getId(), SecureTokens.hash(rawToken), Instant.now().plus(TOKEN_TTL)));
        events.publishEvent(new EmailVerificationRequested(
            user.getId(), user.getEmail(), user.getDisplayName(), rawToken));
    }

    // Hesabım'daki "Doğrula": yeni bağlantı gönderir, eskileri geçersiz kılar. Dakikada en fazla bir e-posta.
    @Transactional
    public void resendVerification(UUID userId) {
        // Kilit transaction sonuna kadar: eşzamanlı iki istek "kontrol et ve gönder" adımında sıraya girer.
        User user = users.findByIdForUpdate(userId).orElseThrow(UserNotFoundException::new);
        if (user.isEmailVerified()) {
            throw new EmailAlreadyVerifiedException();
        }
        Instant now = Instant.now();
        if (tokens.existsByUserIdAndCreatedAtAfter(userId, now.minus(RESEND_COOLDOWN))) {
            throw new TooManyVerificationEmailsException();
        }
        tokens.invalidateActiveForUser(userId, now);
        requestVerification(user);
    }

    @Transactional
    public void verify(String rawToken) {
        String tokenHash = SecureTokens.hash(rawToken);
        Instant now = Instant.now();

        if (tokens.consumeIfValid(tokenHash, now) == 0) {
            throw new InvalidVerificationTokenException();
        }
        EmailVerificationToken token = tokens.findByTokenHash(tokenHash).orElseThrow();
        users.findById(token.getUserId())
            .orElseThrow(InvalidVerificationTokenException::new)
            .verifyEmail(now);
    }
}
