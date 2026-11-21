package com.venyu.venue;

import java.util.Optional;
import java.util.UUID;

/**
 * Diğer modüllere açık sorgu API'si (ADR-007). Uygulaması venue modülünün içinde.
 */
public interface VenueBookings {

    /**
     * Rezervasyon CONFIRMED ve bu organizatöre aitse döner; aksi halde (yok, onaylanmamış, başkasının) boş.
     */
    Optional<ConfirmedVenueBooking> findConfirmed(UUID bookingId, UUID organizerId);
}
