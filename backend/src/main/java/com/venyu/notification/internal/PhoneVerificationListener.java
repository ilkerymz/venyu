package com.venyu.notification.internal;

import com.venyu.identity.PhoneVerificationRequested;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

// Geliştirme ortamı: gerçek SMS yerine log. SMS sağlayıcısı (Netgsm vb.) geldiğinde sadece bu sınıf değişir.
@Slf4j
@Component
class PhoneVerificationListener {

    @ApplicationModuleListener
    void on(PhoneVerificationRequested event) {
        log.info("[DEV SMS] To: {} | Venyu doğrulama kodun: {} (5 dk geçerli)", event.phone(), event.code());
    }
}