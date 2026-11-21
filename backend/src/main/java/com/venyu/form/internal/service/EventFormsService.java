package com.venyu.form.internal.service;

import com.venyu.form.EventForm;
import com.venyu.form.EventForms;
import com.venyu.form.FieldType;
import com.venyu.form.FormField;
import com.venyu.form.FormValidation;
import com.venyu.form.internal.repository.FormSchemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class EventFormsService implements EventForms {

    private static final int TEXT_MAX = 500;
    private static final int LONG_TEXT_MAX = 5000;

    private final FormSchemaRepository schemas;
    private final FormDefinitions definitions;
    private final FormRules rules;

    @Override
    @Transactional(readOnly = true)
    public Optional<EventForm> findCurrent(UUID eventId) {
        return schemas.findFirstByEventIdOrderByVersionDesc(eventId)
            .map(schema -> new EventForm(schema.getVersion(), definitions.read(schema.getDefinition())))
            .filter(form -> !form.fields().isEmpty());
    }

    @Override
    @Transactional(readOnly = true)
    public FormValidation validate(UUID eventId, UUID ticketTypeId, Map<String, Object> answers) {
        Map<String, Object> given = answers == null ? Map.of() : answers;
        Optional<EventForm> current = findCurrent(eventId);
        if (current.isEmpty()) {
            return given.isEmpty()
                ? new FormValidation.Valid(null, Map.of())
                : new FormValidation.Invalid(List.of("Bu etkinliğin formu yok; cevap gönderilmemeli."));
        }
        EventForm form = current.get();
        List<String> errors = new ArrayList<>();

        Set<String> known = new HashSet<>();
        form.fields().forEach(field -> known.add(field.key()));
        given.keySet().stream().filter(key -> !known.contains(key)).sorted()
            .forEach(key -> errors.add(key + ": formda böyle bir alan yok."));

        // Alanlar sırayla değerlendirilir: showIf sadece önceki alanlara bakar (kaydederken garanti edildi).
        Map<String, Object> seen = new HashMap<>();   // JEXL context'i: gizli/boş alan null
        Map<String, Object> cleaned = new LinkedHashMap<>(); // saklanacak hali: sadece görünen ve dolu alanlar
        for (FormField field : form.fields()) {
            seen.put(field.key(), null);
            if (!rules.isVisible(field, ticketTypeId, seen)) {
                continue; // gizli alanın cevabı (gönderildiyse) atılır
            }
            Object value = given.get(field.key());
            if (isEmpty(value)) {
                if (field.required()) {
                    errors.add(field.key() + ": zorunlu.");
                }
                continue;
            }
            String error = checkValue(field, value);
            if (error != null) {
                errors.add(field.key() + ": " + error);
                continue;
            }
            if (field.type() == FieldType.BOOLEAN && field.required() && !Boolean.TRUE.equals(value)) {
                errors.add(field.key() + ": onaylanmalı.");
                continue;
            }
            seen.put(field.key(), value);
            cleaned.put(field.key(), value);
        }
        return errors.isEmpty()
            ? new FormValidation.Valid(form.version(), cleaned)
            : new FormValidation.Invalid(errors);
    }

    private static boolean isEmpty(Object value) {
        return value == null
            || (value instanceof String text && text.isBlank())
            || (value instanceof List<?> list && list.isEmpty());
    }

    // Hata mesajı veya null.
    private static String checkValue(FormField field, Object value) {
        return switch (field.type()) {
            case TEXT -> value instanceof String text && text.length() <= TEXT_MAX
                ? null : "en fazla " + TEXT_MAX + " karakterlik metin olmalı.";
            case LONG_TEXT -> value instanceof String text && text.length() <= LONG_TEXT_MAX
                ? null : "en fazla " + LONG_TEXT_MAX + " karakterlik metin olmalı.";
            case NUMBER -> value instanceof Number ? null : "sayı olmalı.";
            case BOOLEAN -> value instanceof Boolean ? null : "true veya false olmalı.";
            case SINGLE_CHOICE -> value instanceof String choice && field.options().contains(choice)
                ? null : "seçeneklerden biri olmalı.";
            case MULTI_CHOICE -> value instanceof List<?> choices
                && field.options().containsAll(choices) && new HashSet<>(choices).size() == choices.size()
                ? null : "seçeneklerden tekrarsız bir liste olmalı.";
        };
    }
}
