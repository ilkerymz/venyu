package com.venyu.identity.internal.service;

import java.nio.charset.StandardCharsets;

final class PasswordRules {

    private static final int BCRYPT_MAX_BYTES = 72;

    private PasswordRules() {
    }

    static boolean exceedsBcryptLimit(String rawPassword) {
        return rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
    }
}