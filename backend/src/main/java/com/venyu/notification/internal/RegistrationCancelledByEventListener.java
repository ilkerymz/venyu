package com.venyu.notification.internal;

import com.venyu.registration.RegistrationCancelledByEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

// Geliştirme ortamı: gerçek e-posta yerine log.
@Slf4j
@Component
class RegistrationCancelledByEventListener {

    @ApplicationModuleListener
    void on(RegistrationCancelledByEvent event) {
        log.info("[DEV EMAIL] To: {} | Merhaba {}, \"{}\" ({}) etkinliği iptal edildi. Kaydın iptal edildi.{}",
            event.email(), event.name(), event.eventTitle(), event.eventStartsAt(),
            event.reason() == null ? "" : " Organizatörün notu: " + event.reason());
    }
}
