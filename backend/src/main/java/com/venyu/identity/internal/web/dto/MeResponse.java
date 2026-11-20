package com.venyu.identity.internal.web.dto;

import java.util.List;
import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        String displayName,
        List<String> roles,
        boolean emailVerified,
        String phone,
        boolean phoneVerified) {
}