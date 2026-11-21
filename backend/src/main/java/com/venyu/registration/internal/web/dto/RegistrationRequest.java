package com.venyu.registration.internal.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

// answers: etkinliğin formu varsa alan key'i → cevap; formsuz etkinlikte boş veya gönderilmez.
public record RegistrationRequest(@NotNull UUID ticketTypeId, @Size(max = 50) Map<String, Object> answers) {
}
