package com.venyu.registration.internal.service;

import com.venyu.event.EventCancelled;
import com.venyu.identity.UserContact;
import com.venyu.identity.UserDirectory;
import com.venyu.registration.RegistrationCancelledByEvent;
import com.venyu.registration.internal.domain.Registration;
import com.venyu.registration.internal.domain.RegistrationStatus;
import com.venyu.registration.internal.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Etkinlik iptal edilince aktif kayıtları iptal eder ve katılımcı başına bildirim yayınlar.
 * İptal commit olduktan sonra, kendi transaction'ında (async) çalışır. Idempotent: outbox aynı event'i ikinci kez
 * teslim ederse iptal edilecek kayıt kalmamıştır, ikinci bildirim gitmez.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class EventCancellationListener {

    private final RegistrationRepository registrations;
    private final UserDirectory userDirectory;
    private final ApplicationEventPublisher events;

    @ApplicationModuleListener
    void on(EventCancelled cancelled) {
        List<Registration> affected = registrations
            .findByEventIdAndStatusIn(cancelled.eventId(), RegistrationStatus.CANCELLED_WITH_EVENT);
        if (affected.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        affected.forEach(registration -> registration.cancelWithEvent(now));
        // Hata olursa (örn. katılımcı aynı anda kendi kaydını iptal etti → optimistic lock) transaction geri alınır,
        // event outbox'ta tamamlanmamış kalır ve yeniden denenir.
        registrations.flush();

        Map<UUID, UserContact> members = userDirectory.findContacts(
            affected.stream().map(Registration::getUserId).filter(Objects::nonNull).distinct().toList());
        for (Registration registration : affected) {
            String email = registration.getGuestEmail();
            String name = registration.getGuestName();
            if (registration.getUserId() != null) {
                UserContact contact = members.get(registration.getUserId());
                email = contact == null ? null : contact.email();
                name = contact == null ? null : contact.displayName();
            }
            if (email == null) {
                continue; // hesabı silinmiş üye: kayıt iptal edildi, bildirilecek adres yok
            }
            events.publishEvent(new RegistrationCancelledByEvent(registration.getId(), email, name,
                cancelled.title(), cancelled.startsAt(), cancelled.reason()));
        }
        log.info("Cancelled {} registrations of cancelled event {}", affected.size(), cancelled.eventId());
    }
}
