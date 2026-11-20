package com.venyu.identity.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// E.164: + ile başlar, ülke kodu 0 ile başlamaz, toplam 8-15 rakam (örn. +905551234567).
public record PhoneCodeRequest(@NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phone) {
}