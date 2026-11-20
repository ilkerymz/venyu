package com.venyu.identity.internal.service;

import com.venyu.identity.internal.domain.User;
import com.venyu.identity.internal.exception.UserNotFoundException;
import com.venyu.identity.internal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository users;

    @Transactional(readOnly = true)
    public User getProfile(UUID userId) {
        return users.findById(userId).orElseThrow(UserNotFoundException::new);
    }
}