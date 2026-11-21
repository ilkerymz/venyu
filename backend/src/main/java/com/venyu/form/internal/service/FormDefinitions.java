package com.venyu.form.internal.service;

import com.venyu.form.FormField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

// form_schemas.definition kolonunun JSON biçimi. Kök nesne ileride alan eklemeye (örn. form başlığı) yer bırakır.
@Component
@RequiredArgsConstructor
class FormDefinitions {

    private final JsonMapper json;

    String write(List<FormField> fields) {
        return json.writeValueAsString(new Definition(fields));
    }

    List<FormField> read(String definition) {
        return json.readValue(definition, Definition.class).fields();
    }

    record Definition(List<FormField> fields) {
    }
}
