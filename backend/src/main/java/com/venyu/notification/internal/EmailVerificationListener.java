package com.venyu.notification.internal;

import com.venyu.identity.EmailVerificationRequested;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

// Geliştirme ortamı: gerçek e-posta yerine log. Gerçek sağlayıcı (SMTP vb.) geldiğinde sadece bu sınıf değişir.
@Slf4j
@Component
@RequiredArgsConstructor
class EmailVerificationListener {

    private final NotificationProperties properties;

    @ApplicationModuleListener
    void on(EmailVerificationRequested event) {
        log.info("[DEV EMAIL] To: {} | Merhaba {}, e-postanı doğrulamak için: {}/verify-email?token={}",
                event.email(), event.displayName(), properties.frontendUrl(), event.token());
    }
}