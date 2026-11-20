package com.venyu.venue.internal.service;

import com.venyu.venue.internal.domain.VenueSlot;
import com.venyu.venue.internal.exception.InvalidSlotException;
import com.venyu.venue.internal.exception.SlotNotFoundException;
import com.venyu.venue.internal.exception.SlotOverlapException;
import com.venyu.venue.internal.repository.VenueSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VenueSlotService {

    private static final Duration MAX_SLOT_LENGTH = Duration.ofHours(24);
    private static final Duration DEFAULT_LIST_RANGE = Duration.ofDays(30);

    private final VenueSlotRepository slots;
    private final VenueService venueService;

    @Transactional
    public VenueSlot open(UUID userId, UUID venueId, Instant startsAt, Instant endsAt) {
        venueService.getOwnedVenue(userId, venueId);
        validate(startsAt, endsAt);
        try {
            return slots.saveAndFlush(VenueSlot.open(venueId, startsAt, endsAt));
        } catch (DataIntegrityViolationException e) {
            // Mekan sahiplik kontrolünden geçtiği ve zaman sırası doğrulandığı için
            // buraya düşen ihlal pratikte venue_slots_no_overlap'tir.
            throw new SlotOverlapException();
        }
    }

    @Transactional
    public void close(UUID userId, UUID venueId, UUID slotId) {
        venueService.getOwnedVenue(userId, venueId);
        VenueSlot slot = slots.findByIdAndVenueId(slotId, venueId).orElseThrow(SlotNotFoundException::new);
        slots.delete(slot);
    }

    @Transactional(readOnly = true)
    public List<VenueSlot> list(UUID venueId, Instant from, Instant to) {
        venueService.get(venueId);
        Instant start = from != null ? from : Instant.now();
        Instant end = to != null ? to : start.plus(DEFAULT_LIST_RANGE);
        return slots.findInRange(venueId, start, end);
    }

    private static void validate(Instant startsAt, Instant endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new InvalidSlotException("Bitiş zamanı başlangıçtan sonra olmalı.");
        }
        if (startsAt.isBefore(Instant.now())) {
            throw new InvalidSlotException("Geçmiş bir zamana slot açılamaz.");
        }
        if (Duration.between(startsAt, endsAt).compareTo(MAX_SLOT_LENGTH) > 0) {
            throw new InvalidSlotException("Bir slot en fazla 24 saat olabilir.");
        }
    }
}