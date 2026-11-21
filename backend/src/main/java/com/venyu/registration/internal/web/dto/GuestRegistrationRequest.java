package com.venyu.registration.internal.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record GuestRegistrationRequest(
    @NotNull UUID ticketTypeId,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(max = 100) String name) {
}
