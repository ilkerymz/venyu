package com.venyu.registration;

import java.time.Instant;
import java.util.UUID;

// Misafirin bileti hazır (e-posta onayı veya davet): notification bilet kodunu ve "yönet" link'ini gönderir.
// manageToken ham haliyle sadece bu event'te yaşar (ADR-004 ile aynı kabul).
public record TicketIssued(UUID registrationId, String email, String name, String eventTitle, Instant eventStartsAt,
                           String ticketCode, String manageToken) {
}
