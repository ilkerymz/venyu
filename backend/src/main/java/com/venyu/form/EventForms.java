package com.venyu.form;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Diğer modüllere açık form API'si (ADR-009).
 */
public interface EventForms {

    // Etkinliğin güncel formu; hiç tanımlanmamışsa veya son versiyon alansızsa boş.
    Optional<EventForm> findCurrent(UUID eventId);

    // Sunucu görünürlüğü kendisi hesaplar: gizli alanların cevapları atılır, görünen zorunlu alanlar aranır.
    FormValidation validate(UUID eventId, UUID ticketTypeId, Map<String, Object> answers);
}
