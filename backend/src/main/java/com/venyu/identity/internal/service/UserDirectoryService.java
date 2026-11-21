package com.venyu.identity.internal.service;

import com.venyu.identity.UserContact;
import com.venyu.identity.UserDirectory;
import com.venyu.identity.internal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class UserDirectoryService implements UserDirectory {

    private final UserRepository users;

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, UserContact> findContacts(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return users.findContacts(userIds).stream()
            .collect(Collectors.toMap(UserContact::id, Function.identity()));
    }
}
