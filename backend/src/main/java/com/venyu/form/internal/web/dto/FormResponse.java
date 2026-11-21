package com.venyu.form.internal.web.dto;

import com.venyu.form.EventForm;
import com.venyu.form.FormField;

import java.util.List;
import java.util.Optional;

// Etkinliğin formu yoksa version null, fields boş.
public record FormResponse(Integer version, List<FormField> fields) {

    public static FormResponse from(Optional<EventForm> form) {
        return form.map(f -> new FormResponse(f.version(), f.fields()))
            .orElseGet(() -> new FormResponse(null, List.of()));
    }
}
