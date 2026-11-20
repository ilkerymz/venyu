package com.venyu.venue.internal.repository;

import com.venyu.venue.internal.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VenueRepository extends JpaRepository<Venue, UUID> {

    List<Venue> findAllByOrderByNameAsc();

    List<Venue> findByDistrictOrderByNameAsc(String district);
}