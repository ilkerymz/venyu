package com.venyu.notification.internal;

import com.venyu.registration.GuestConfirmationRequested;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

// Geliştirme ortamı: gerçek e-posta yerine log (EmailVerificationListener ile aynı).
@Slf4j
@Component
@RequiredArgsConstructor
class GuestConfirmationListener {

    private final NotificationProperties properties;

    @ApplicationModuleListener
    void on(GuestConfirmationRequested event) {
        log.info("[DEV EMAIL] To: {} | Merhaba {}, \"{}\" kaydını {} tarihine kadar onayla: {}/registrations/confirm?token={}",
            event.email(), event.name(), event.eventTitle(), event.expiresAt(), properties.frontendUrl(), event.token());
    }
}
