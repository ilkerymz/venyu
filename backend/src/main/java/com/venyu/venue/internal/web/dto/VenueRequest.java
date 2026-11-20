package com.venyu.venue.internal.web.dto;

import com.venyu.venue.internal.domain.VenueDetails;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VenueRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 300) String address,
        @NotBlank @Size(max = 100) String district,
        @NotBlank @Size(max = 100) String city,
        @Min(1) @Max(10_000) int capacity) {

    public VenueDetails toDetails() {
        return new VenueDetails(name, description, address, district, city, capacity);
    }
}