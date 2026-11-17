package com.venyu.identity.internal.web.dto;

import java.util.List;
import java.util.UUID;

public record MeResponse(UUID id, List<String> roles) {
}