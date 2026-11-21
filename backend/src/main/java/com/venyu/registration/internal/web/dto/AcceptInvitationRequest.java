package com.venyu.registration.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AcceptInvitationRequest(@NotBlank String token, @NotNull UUID ticketTypeId) {
}
