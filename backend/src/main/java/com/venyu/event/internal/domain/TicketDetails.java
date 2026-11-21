package com.venyu.event.internal.domain;

import java.math.BigDecimal;

// Capacity null = sınırsız.
public record TicketDetails(String name, String description, BigDecimal price, Integer capacity) {
}
