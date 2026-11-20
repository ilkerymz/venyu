package com.venyu.identity;

import java.util.UUID;

public record PasswordResetRequested(UUID userId, String email, String displayName, String token) {
}