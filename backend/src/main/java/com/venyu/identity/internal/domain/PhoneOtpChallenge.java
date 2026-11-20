package com.venyu.identity.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "phone_otp_challenges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneOtpChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, updatable = false)
    private String phone;

    @Column(nullable = false, updatable = false)
    private String codeHash;

    @Column(nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;

    private Instant consumedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static PhoneOtpChallenge issue(UUID userId, String phone, String codeHash, Instant expiresAt) {
        PhoneOtpChallenge challenge = new PhoneOtpChallenge();
        challenge.userId = userId;
        challenge.phone = phone;
        challenge.codeHash = codeHash;
        challenge.expiresAt = expiresAt;
        challenge.createdAt = Instant.now();
        return challenge;
    }
}