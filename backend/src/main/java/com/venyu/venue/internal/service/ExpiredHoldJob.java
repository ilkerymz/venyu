package com.venyu.venue.internal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class ExpiredHoldJob {

    private final VenueBookingService bookingService;

    // fixedDelay: bir tur bitince 1 dk bekler (üst üste binmez). Kilit: aynı anda tek instance.
    @Scheduled(fixedDelayString = "PT1M")
    @SchedulerLock(name = "venue.expireHolds", lockAtMostFor = "PT5M", lockAtLeastFor = "PT10S")
    public void run() {
        int expired = bookingService.expireHolds();
        if (expired > 0) {
            log.info("Expired {} venue booking holds", expired);
        }
    }
}