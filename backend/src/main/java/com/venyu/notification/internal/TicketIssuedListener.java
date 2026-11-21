package com.venyu.notification.internal;

import com.venyu.registration.TicketIssued;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

// Geliştirme ortamı: gerçek e-posta yerine log. Gerçek sağlayıcıda QR görseli burada üretilir (ZXing, ADR-010).
@Slf4j
@Component
@RequiredArgsConstructor
class TicketIssuedListener {

    private final NotificationProperties properties;

    @ApplicationModuleListener
    void on(TicketIssued event) {
        log.info("[DEV EMAIL] To: {} | Merhaba {}, \"{}\" ({}) biletin hazır. QR kodu: {} | Kaydını yönet: {}/registrations/manage?token={}",
            event.email(), event.name(), event.eventTitle(), event.eventStartsAt(), event.ticketCode(),
            properties.frontendUrl(), event.manageToken());
    }
}
