package com.venyu.registration.internal.web.dto;

import jakarta.validation.constraints.NotBlank;

// Token body'de: URL'de olsa sunucu/proxy log'larına ve Referer'a düşerdi.
public record ManageTokenRequest(@NotBlank String token) {
}
