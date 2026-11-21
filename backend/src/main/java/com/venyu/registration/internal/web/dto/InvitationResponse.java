package com.venyu.registration.internal.web.dto;

import com.venyu.registration.internal.domain.Invitation;
import com.venyu.registration.internal.domain.InvitationStatus;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(UUID id, String email, InvitationStatus status, Instant createdAt,
                                 Instant acceptedAt) {

    public static InvitationResponse from(Invitation invitation) {
        return new InvitationResponse(invitation.getId(), invitation.getEmail(), invitation.getStatus(),
            invitation.getCreatedAt(), invitation.getAcceptedAt());
    }
}
