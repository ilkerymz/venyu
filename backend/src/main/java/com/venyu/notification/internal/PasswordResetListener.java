package com.venyu.notification.internal;

import com.venyu.identity.PasswordResetRequested;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

// Geliştirme ortamı: gerçek e-posta yerine log.
@Slf4j
@Component
@RequiredArgsConstructor
class PasswordResetListener {

    private final NotificationProperties properties;

    @ApplicationModuleListener
    void on(PasswordResetRequested event) {
        log.info("[DEV EMAIL] To: {} | Merhaba {}, şifreni sıfırlamak için (30 dk geçerli): {}/reset-password?token={}",
                event.email(), event.displayName(), properties.frontendUrl(), event.token());
    }
}