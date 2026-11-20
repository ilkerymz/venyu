package com.venyu.identity.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneCodeConfirmRequest(@NotBlank @Pattern(regexp = "^\\d{6}$") String code) {
}