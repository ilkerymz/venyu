package com.venyu.identity.internal.service;

import com.venyu.identity.PhoneVerificationRequested;
import com.venyu.identity.internal.domain.PhoneOtpChallenge;
import com.venyu.identity.internal.exception.InvalidOtpCodeException;
import com.venyu.identity.internal.exception.PhoneAlreadyInUseException;
import com.venyu.identity.internal.exception.TooManyOtpRequestsException;
import com.venyu.identity.internal.repository.PhoneOtpChallengeRepository;
import com.venyu.identity.internal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(10);
    private static final int MAX_REQUESTS_PER_WINDOW = 3;
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PhoneOtpChallengeRepository challenges;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    @Transactional
    public void requestCode(UUID userId, String phone) {
        // Kilit transaction sonuna kadar tutulur: aynı kullanıcının "say ve ekle" adımları sıraya girer.
        users.findByIdForUpdate(userId).orElseThrow();

        Instant now = Instant.now();
        if (challenges.countByUserIdAndCreatedAtAfter(userId, now.minus(RATE_LIMIT_WINDOW)) >= MAX_REQUESTS_PER_WINDOW) {
            throw new TooManyOtpRequestsException();
        }
        if (users.existsByPhoneAndPhoneVerifiedAtIsNotNullAndIdNot(phone, userId)) {
            throw new PhoneAlreadyInUseException();
        }

        challenges.invalidateActiveForUser(userId, now);
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        challenges.save(PhoneOtpChallenge.issue(userId, phone, passwordEncoder.encode(code), now.plus(CODE_TTL)));
        events.publishEvent(new PhoneVerificationRequested(userId, phone, code));
    }

    @Transactional(noRollbackFor = InvalidOtpCodeException.class)
    public void confirmCode(UUID userId, String code) {
        Instant now = Instant.now();
        PhoneOtpChallenge challenge = challenges.findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(userId)
                .orElseThrow(InvalidOtpCodeException::new);

        // Önce hakkı düş (atomik), sonra kontrol et: yanlış kodda da sayaç commit edilir.
        if (challenges.registerAttempt(challenge.getId(), MAX_ATTEMPTS, now) == 0
                || !passwordEncoder.matches(code, challenge.getCodeHash())) {
            throw new InvalidOtpCodeException();
        }
        if (challenges.consume(challenge.getId(), now) == 0) {
            throw new InvalidOtpCodeException();
        }

        if (users.existsByPhoneAndPhoneVerifiedAtIsNotNullAndIdNot(challenge.getPhone(), userId)) {
            throw new PhoneAlreadyInUseException();
        }
        try {
            users.findById(userId).orElseThrow().verifyPhone(challenge.getPhone(), now);
            users.flush();
        } catch (DataIntegrityViolationException e) {
            // İki kullanıcı aynı numarayı aynı anda doğrularsa son garanti: users_verified_phone_uq.
            throw new PhoneAlreadyInUseException();
        }
    }
}