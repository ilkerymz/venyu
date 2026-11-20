package com.venyu.venue.internal.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

// Saat dilimi bilgisiyle alınır (örn. "2026-12-01T19:00:00+03:00"), DB'ye UTC Instant olarak yazılır.
public record SlotRequest(@NotNull OffsetDateTime startsAt, @NotNull OffsetDateTime endsAt) {
}