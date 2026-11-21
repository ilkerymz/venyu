package com.venyu.registration.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// QR'dan okunan bilet kodu.
public record CheckInRequest(@NotBlank @Size(max = 64) String ticketCode) {
}
