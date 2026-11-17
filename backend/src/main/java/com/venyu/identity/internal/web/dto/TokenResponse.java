package com.venyu.identity.internal.web.dto;

public record TokenResponse(String accessToken, String refreshToken, long expiresIn) {
}
