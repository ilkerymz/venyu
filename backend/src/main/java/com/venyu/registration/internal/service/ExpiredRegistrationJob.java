package com.venyu.registration.internal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class ExpiredRegistrationJob {

    private final RegistrationService registrationService;

    // venue.ExpiredHoldJob ile aynı desen: fixedDelay (üst üste binmez) + ShedLock (tek instance).
    @Scheduled(fixedDelayString = "PT1M")
    @SchedulerLock(name = "registration.expireReservations", lockAtMostFor = "PT5M", lockAtLeastFor = "PT10S")
    public void run() {
        int expired = registrationService.expireReservations();
        if (expired > 0) {
            log.info("Expired {} unconfirmed guest registrations", expired);
        }
    }
}
