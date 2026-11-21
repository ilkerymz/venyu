package com.venyu.event.internal.web.dto;

import com.venyu.event.internal.domain.EventDetails;
import com.venyu.event.internal.domain.EventVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateEventRequest(
    @NotBlank @Size(max = 150) String title,
    @Size(max = 5000) String description,
    @NotNull EventVisibility visibility,
    Boolean requiresAccount) {

    public EventDetails toDetails() {
        return new EventDetails(title, description, visibility, Boolean.TRUE.equals(requiresAccount));
    }
}
