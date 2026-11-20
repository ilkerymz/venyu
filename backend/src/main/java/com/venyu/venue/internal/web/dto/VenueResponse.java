package com.venyu.venue.internal.web.dto;

import com.venyu.venue.internal.domain.Venue;

import java.util.UUID;

public record VenueResponse(
        UUID id,
        UUID ownerId,
        String name,
        String description,
        String address,
        String district,
        String city,
        int capacity) {

    public static VenueResponse from(Venue venue) {
        return new VenueResponse(venue.getId(), venue.getOwnerId(), venue.getName(), venue.getDescription(),
                venue.getAddress(), venue.getDistrict(), venue.getCity(), venue.getCapacity());
    }
}