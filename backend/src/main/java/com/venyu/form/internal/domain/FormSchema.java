package com.venyu.form.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;
import java.util.UUID;

// Değişmez: her kaydetme yeni versiyon (satır) üretir, mevcut satır hiç güncellenmez.
@Entity
@Table(name = "form_schemas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FormSchema {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false, updatable = false)
    private int version;

    // JSONB kolonu Java'da String: (de)serialization FormDefinitions'ta, Jackson 3 ile.
    // ?::jsonb olmadan PostgreSQL text → jsonb atamasını reddeder.
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String definition;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static FormSchema create(UUID eventId, int version, String definition, UUID createdBy) {
        FormSchema schema = new FormSchema();
        schema.eventId = eventId;
        schema.version = version;
        schema.definition = definition;
        schema.createdBy = createdBy;
        schema.createdAt = Instant.now();
        return schema;
    }
}
