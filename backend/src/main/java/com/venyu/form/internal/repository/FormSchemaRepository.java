package com.venyu.form.internal.repository;

import com.venyu.form.internal.domain.FormSchema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FormSchemaRepository extends JpaRepository<FormSchema, UUID> {

    // form_schemas_event_version_uq index'i (event_id, version) bu sorguyu karşılar.
    Optional<FormSchema> findFirstByEventIdOrderByVersionDesc(UUID eventId);
}
