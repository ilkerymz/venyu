package com.venyu.identity;

import java.util.UUID;

public record PhoneVerificationRequested(UUID userId, String phone, String code) {
}