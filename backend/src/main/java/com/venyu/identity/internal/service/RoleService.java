package com.venyu.identity.internal.service;

import com.venyu.identity.internal.domain.Role;
import com.venyu.identity.internal.exception.UserNotFoundException;
import com.venyu.identity.internal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final UserRepository users;

    @Transactional
    public void becomeOrganizer(UUID userId) {
        grant(userId, Role.ORGANIZER);
    }

    // VENUE_OWNER kendi kendine alınamaz: pilot mekanları elle eklenir.
    // Şimdilik testler kullanıyor; ileride admin paneli bu metodu çağıracak.
    @Transactional
    public void grantVenueOwner(UUID userId) {
        grant(userId, Role.VENUE_OWNER);
    }

    private void grant(UUID userId, Role role) {
        users.findById(userId).orElseThrow(UserNotFoundException::new).grantRole(role);
    }
}