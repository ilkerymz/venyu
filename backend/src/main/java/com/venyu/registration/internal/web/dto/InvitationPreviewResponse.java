package com.venyu.registration.internal.web.dto;

import com.venyu.event.TicketOffer;
import com.venyu.form.EventForm;
import com.venyu.form.FormField;
import com.venyu.registration.internal.domain.InvitationStatus;
import com.venyu.registration.internal.service.InvitationService.InvitationPreview;

import java.time.Instant;
import java.util.List;

// requiresAccount true ise frontend misafir seçeneğini göstermez.
// INVITE_ONLY etkinliğin formu public GET /events/{id}/form'dan okunamaz; burada döner (formVersion null = formsuz).
public record InvitationPreviewResponse(
    String email,
    InvitationStatus status,
    String eventTitle,
    String eventSlug,
    Instant startsAt,
    Instant endsAt,
    boolean requiresAccount,
    List<TicketOffer> tickets,
    Integer formVersion,
    List<FormField> formFields) {

    public static InvitationPreviewResponse from(InvitationPreview preview) {
        EventForm form = preview.form();
        return new InvitationPreviewResponse(preview.invitation().getEmail(), preview.invitation().getStatus(),
            preview.event().title(), preview.event().slug(), preview.event().startsAt(), preview.event().endsAt(),
            preview.event().requiresAccount(), preview.tickets(),
            form == null ? null : form.version(), form == null ? List.of() : form.fields());
    }
}
