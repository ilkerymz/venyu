package com.venyu.form.internal.web.dto;

import com.venyu.form.FormField;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// Alanların sırası önemli: form bu sırayla gösterilir ve showIf sadece öncekilere bakabilir.
public record FormRequest(@NotNull @Size(max = 50) List<@NotNull @Valid FormField> fields) {
}
