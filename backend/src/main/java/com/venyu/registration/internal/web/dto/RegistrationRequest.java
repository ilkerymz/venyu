package com.venyu.registration.internal.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegistrationRequest(@NotNull UUID ticketTypeId) {
}
