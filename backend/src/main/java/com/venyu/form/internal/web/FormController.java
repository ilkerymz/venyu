package com.venyu.form.internal.web;

import com.venyu.form.internal.service.FormService;
import com.venyu.form.internal.web.dto.FormRequest;
import com.venyu.form.internal.web.dto.FormResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
class FormController {

    private final FormService formService;

    // PUT: formun tamamı gönderilir, yeni versiyon oluşur (alan bazında PATCH yok).
    @PutMapping("/events/{eventId}/form")
    @PreAuthorize("hasRole('ORGANIZER')")
    FormResponse save(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId,
                      @Valid @RequestBody FormRequest request) {
        return FormResponse.from(formService.save(UUID.fromString(jwt.getSubject()), eventId, request.fields()));
    }

    // Public: jwt yoksa anonim ziyaretçi.
    @GetMapping("/events/{eventId}/form")
    FormResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId) {
        UUID viewerId = jwt == null ? null : UUID.fromString(jwt.getSubject());
        return FormResponse.from(formService.get(viewerId, eventId));
    }
}
