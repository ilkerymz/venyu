package com.venyu.registration;

import java.time.Instant;
import java.util.UUID;

// Misafire onay link'i gönderilsin (notification dinler). token ham haliyle sadece bu event'te yaşar.
public record GuestConfirmationRequested(UUID registrationId, String email, String name, String eventTitle,
                                         String token, Instant expiresAt) {
}
