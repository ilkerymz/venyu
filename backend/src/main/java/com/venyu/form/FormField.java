package com.venyu.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Formdaki bir soru. Hem organizatörün gönderdiği şemanın hem de katılımcıya gösterilen formun parçası.
 *
 * @param showIf        JEXL ifadesi; boşsa her zaman görünür. Sadece önceki alanlara ({@code answers.<key>})
 *                      ve {@code ticketTypeId}'ye başvurabilir.
 * @param ticketTypeIds boşsa bütün bilet türlerinde sorulur.
 */
public record FormField(
    @NotBlank @Pattern(regexp = "[a-z][a-z0-9_]{0,39}") String key,
    @NotBlank @Size(max = 200) String label,
    @NotNull FieldType type,
    Boolean required,
    @Size(max = 50) List<@NotBlank @Size(max = 100) String> options,
    @Size(max = 500) String showIf,
    @Size(max = 20) List<@NotNull UUID> ticketTypeIds) {

    // Eksik alanlar tek tip hale gelir (JSON'da gönderilmemiş olabilirler).
    public FormField {
        required = Boolean.TRUE.equals(required);
        options = options == null ? List.of() : List.copyOf(options);
        showIf = showIf == null || showIf.isBlank() ? null : showIf.strip();
        ticketTypeIds = ticketTypeIds == null ? List.of() : List.copyOf(ticketTypeIds);
    }

    public boolean isChoice() {
        return type == FieldType.SINGLE_CHOICE || type == FieldType.MULTI_CHOICE;
    }
}
