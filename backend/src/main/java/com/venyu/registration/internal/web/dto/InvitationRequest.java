package com.venyu.registration.internal.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record InvitationRequest(
    @NotEmpty @Size(max = 100) List<@NotBlank @Email @Size(max = 254) String> emails) {
}
