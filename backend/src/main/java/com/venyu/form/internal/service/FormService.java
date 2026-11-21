package com.venyu.form.internal.service;

import com.venyu.event.EventLookup;
import com.venyu.event.EventSummary;
import com.venyu.event.TicketOffer;
import com.venyu.form.EventForm;
import com.venyu.form.EventForms;
import com.venyu.form.FormField;
import com.venyu.form.internal.domain.FormSchema;
import com.venyu.form.internal.exception.FormConflictException;
import com.venyu.form.internal.exception.FormOrganizerOnlyException;
import com.venyu.form.internal.exception.FormStateException;
import com.venyu.form.internal.exception.FormTargetNotFoundException;
import com.venyu.form.internal.exception.InvalidFormException;
import com.venyu.form.internal.repository.FormSchemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FormService {

    private final FormSchemaRepository schemas;
    private final FormDefinitions definitions;
    private final FormRules rules;
    private final EventForms eventForms;
    private final EventLookup eventLookup;

    // Her kaydetme yeni versiyon. Boş alan listesi = formu kaldırmak (kayıtlar formsuz devam eder).
    @Transactional
    public Optional<EventForm> save(UUID organizerId, UUID eventId, List<FormField> fields) {
        EventSummary event = eventLookup.find(eventId).orElseThrow(FormTargetNotFoundException::new);
        if (!event.isOrganizedBy(organizerId)) {
            throw new FormOrganizerOnlyException();
        }
        if (!event.startsAt().isAfter(Instant.now())) {
            throw new FormStateException("Başlamış veya geçmiş bir etkinliğin formu değiştirilemez.");
        }
        List<String> errors = check(fields, eventLookup.findTicketOffers(eventId));
        if (!errors.isEmpty()) {
            throw new InvalidFormException(errors);
        }
        int nextVersion = schemas.findFirstByEventIdOrderByVersionDesc(eventId)
            .map(schema -> schema.getVersion() + 1).orElse(1);
        try {
            schemas.saveAndFlush(FormSchema.create(eventId, nextVersion, definitions.write(fields), organizerId));
        } catch (DataIntegrityViolationException e) {
            // Aynı etkinliğin formu eşzamanlı kaydedildi: ikisi de aynı versiyonu üretti (form_schemas_event_version_uq).
            throw new FormConflictException();
        }
        return eventForms.findCurrent(eventId);
    }

    // Organizatör her zaman görür; diğerleri sadece herkese açık etkinlikte. Görünmeyen etkinlik 404 (ADR-007).
    @Transactional(readOnly = true)
    public Optional<EventForm> get(UUID viewerId, UUID eventId) {
        EventSummary event = eventLookup.find(eventId).orElseThrow(FormTargetNotFoundException::new);
        boolean organizer = viewerId != null && event.isOrganizedBy(viewerId);
        if (!event.publiclyVisible() && !organizer) {
            throw new FormTargetNotFoundException();
        }
        return eventForms.findCurrent(eventId);
    }

    // Bean Validation'ın göremediği, alanlar arası ve etkinliğe bağlı kurallar. Bütün hatalar birlikte döner.
    private List<String> check(List<FormField> fields, List<TicketOffer> tickets) {
        List<String> errors = new ArrayList<>();
        Set<UUID> ticketIds = new HashSet<>();
        tickets.forEach(ticket -> ticketIds.add(ticket.id()));
        Set<String> earlierKeys = new LinkedHashSet<>();

        for (FormField field : fields) {
            String key = field.key();
            if (earlierKeys.contains(key)) {
                errors.add(key + ": aynı key birden fazla alanda kullanılmış.");
            }
            if (field.isChoice()) {
                if (field.options().isEmpty()) {
                    errors.add(key + ": seçenekli alanın en az bir seçeneği olmalı.");
                }
                if (new HashSet<>(field.options()).size() != field.options().size()) {
                    errors.add(key + ": seçenekler tekrar etmemeli.");
                }
            } else if (!field.options().isEmpty()) {
                errors.add(key + ": sadece seçenekli alanların seçenekleri olabilir.");
            }
            if (!ticketIds.containsAll(field.ticketTypeIds())) {
                errors.add(key + ": bu etkinliğe ait olmayan bilet türü.");
            }
            if (field.showIf() != null) {
                errors.addAll(rules.check(key, field.showIf(), earlierKeys));
            }
            earlierKeys.add(key);
        }
        return errors;
    }
}
