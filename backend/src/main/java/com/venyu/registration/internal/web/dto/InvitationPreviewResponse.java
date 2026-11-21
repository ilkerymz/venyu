package com.venyu.registration.internal.web.dto;

import com.venyu.event.TicketOffer;
import com.venyu.registration.internal.domain.InvitationStatus;
import com.venyu.registration.internal.service.InvitationService.InvitationPreview;

import java.time.Instant;
import java.util.List;

// requiresAccount true ise frontend misafir seçeneğini göstermez.
public record InvitationPreviewResponse(
    String email,
    InvitationStatus status,
    String eventTitle,
    String eventSlug,
    Instant startsAt,
    Instant endsAt,
    boolean requiresAccount,
    List<TicketOffer> tickets) {

    public static InvitationPreviewResponse from(InvitationPreview preview) {
        return new InvitationPreviewResponse(preview.invitation().getEmail(), preview.invitation().getStatus(),
            preview.event().title(), preview.event().slug(), preview.event().startsAt(), preview.event().endsAt(),
            preview.event().requiresAccount(), preview.tickets());
    }
}
