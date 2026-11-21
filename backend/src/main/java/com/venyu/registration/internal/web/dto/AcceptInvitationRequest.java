package com.venyu.registration.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record AcceptInvitationRequest(
    @NotBlank String token,
    @NotNull UUID ticketTypeId,
    @Size(max = 50) Map<String, Object> answers) {
}
