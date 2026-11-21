package com.venyu.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Etkinlik iptal edildi. Registration dinler: aktif kayıtları iptal eder ve katılımcılara bildirim yayınlar.
 * İptalle aynı transaction'da outbox'a yazılır; dinleyici commit'ten sonra, kendi transaction'ında çalışır.
 */
public record EventCancelled(UUID eventId, String title, Instant startsAt, String reason, Instant cancelledAt) {
}
