package com.venyu.identity;

import java.util.UUID;

public record UserContact(UUID id, String displayName, String email) {
}
