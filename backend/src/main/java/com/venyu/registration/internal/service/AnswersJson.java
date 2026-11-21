package com.venyu.registration.internal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

// registrations.answers kolonunun JSON biçimi (form modülündeki FormDefinitions ile aynı yaklaşım).
@Component
@RequiredArgsConstructor
class AnswersJson {

    private final JsonMapper json;

    String write(Map<String, Object> answers) {
        return json.writeValueAsString(answers);
    }
}
