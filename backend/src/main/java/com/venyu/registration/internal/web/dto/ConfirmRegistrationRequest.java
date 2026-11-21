package com.venyu.registration.internal.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmRegistrationRequest(@NotBlank String token) {
}
