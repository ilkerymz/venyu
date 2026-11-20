package com.venyu.venue.internal.domain;

// Mekanın düzenlenebilir alanları: servis ve domain arasında taşınır, web DTO'sundan bağımsız.
public record VenueDetails(
        String name,
        String description,
        String address,
        String district,
        String city,
        int capacity) {
}