package com.venyu.event.internal.domain;

public enum EventStatus {
    DRAFT,
    PUBLISHED,
    CONFIRMED, // F3: minimum katılımcı eşiği geçildi
    COMPLETED,
    SETTLED,   // F3: hakediş ödendi
    CANCELLED
}
