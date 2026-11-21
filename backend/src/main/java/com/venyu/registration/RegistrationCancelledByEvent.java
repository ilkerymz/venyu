package com.venyu.registration;

import java.time.Instant;
import java.util.UUID;

// Etkinlik iptal edildiği için kayıt iptal oldu: notification katılımcıya haber verir. Kayıt başına bir event:
// bir e-posta başarısız olursa outbox sadece onu yeniden dener.
public record RegistrationCancelledByEvent(UUID registrationId, String email, String name, String eventTitle,
                                           Instant eventStartsAt, String reason) {
}
