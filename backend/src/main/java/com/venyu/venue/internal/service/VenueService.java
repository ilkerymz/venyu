package com.venyu.venue.internal.service;

import com.venyu.venue.internal.domain.Venue;
import com.venyu.venue.internal.domain.VenueDetails;
import com.venyu.venue.internal.exception.NotVenueOwnerException;
import com.venyu.venue.internal.exception.VenueNotFoundException;
import com.venyu.venue.internal.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venues;

    @Transactional
    public Venue create(UUID ownerId, VenueDetails details) {
        return venues.save(Venue.create(ownerId, details));
    }

    @Transactional
    public Venue update(UUID userId, UUID venueId, VenueDetails details) {
        Venue venue = getOwnedVenue(userId, venueId);
        venue.update(details);
        return venue;
    }

    @Transactional(readOnly = true)
    public Venue get(UUID venueId) {
        return venues.findById(venueId).orElseThrow(VenueNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<Venue> list(String district) {
        return district == null
                ? venues.findAllByOrderByNameAsc()
                : venues.findByDistrictOrderByNameAsc(district);
    }

    // Slot ve booking işlemleri de bunu kullanacak: "bu mekan senin mi?" tek yerde.
    @Transactional(readOnly = true)
    public Venue getOwnedVenue(UUID userId, UUID venueId) {
        Venue venue = get(venueId);
        if (!venue.isOwnedBy(userId)) {
            throw new NotVenueOwnerException();
        }
        return venue;
    }
}