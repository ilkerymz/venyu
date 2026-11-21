package com.venyu.registration.internal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

// registrations.answers kolonunun JSON biçimi (form modülündeki FormDefinitions ile aynı yaklaşım).
@Component
@RequiredArgsConstructor
class AnswersJson {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };

    private final JsonMapper json;

    String write(Map<String, Object> answers) {
        return json.writeValueAsString(answers);
    }

    Map<String, Object> read(String answers) {
        return json.readValue(answers, MAP);
    }
}
