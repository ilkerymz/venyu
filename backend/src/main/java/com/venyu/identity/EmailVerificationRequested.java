package com.venyu.identity;

import java.util.UUID;

public record EmailVerificationRequested(UUID userId, String email, String displayName, String token) {
}