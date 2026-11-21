package com.venyu.notification.internal;

import com.venyu.registration.InvitationSent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

// Geliştirme ortamı: gerçek e-posta yerine log (EmailVerificationListener ile aynı).
@Slf4j
@Component
@RequiredArgsConstructor
class InvitationListener {

    private final NotificationProperties properties;

    @ApplicationModuleListener
    void on(InvitationSent event) {
        log.info("[DEV EMAIL] To: {} | \"{}\" etkinliğine davetlisin: {}/invitations/accept?token={}",
            event.email(), event.eventTitle(), properties.frontendUrl(), event.token());
    }
}
