package com.venyu.event;

import java.math.BigDecimal;
import java.util.UUID;

// Katılımcıya gösterilen bilet seçeneği: satış sayısı yok, sadece tükendi bilgisi.
public record TicketOffer(UUID id, String name, String description, BigDecimal price, String currency,
                          boolean soldOut) {
}
