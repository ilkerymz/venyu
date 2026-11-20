package com.venyu.identity.internal.service;

import com.venyu.identity.EmailVerificationRequested;
import com.venyu.identity.internal.domain.EmailVerificationToken;
import com.venyu.identity.internal.domain.User;
import com.venyu.identity.internal.exception.InvalidVerificationTokenException;
import com.venyu.identity.internal.repository.EmailVerificationTokenRepository;
import com.venyu.identity.internal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Duration TOKEN_TTL = Duration.ofHours(24);

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