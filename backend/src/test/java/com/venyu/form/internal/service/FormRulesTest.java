package com.venyu.form.internal.service;

import com.venyu.form.FieldType;
import com.venyu.form.FormField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Saf unit test: JEXL sandbox'ı Spring ve DB olmadan.
class FormRulesTest {

    FormRules rules = new FormRules();
    UUID ticket = UUID.randomUUID();

    @Test
    void acceptsComparisonsOnEarlierAnswersAndTicket() {
        Set<String> earlier = Set.of("transport", "diet");

        assertThat(rules.check("plate", "answers.transport == 'car'", earlier)).isEmpty();
        assertThat(rules.check("x", "'vegan' =~ answers.diet && ticketTypeId == '" + ticket + "'", earlier)).isEmpty();
        assertThat(rules.check("x", "!answers.transport", earlier)).isEmpty();
    }

    // Kaydedilmesi reddedilen ifadeler: sandbox'ın asıl güvencesi kaydetme anında.
    @ParameterizedTest
    @ValueSource(strings = {
        "''.getClass()",                       // metot çağrısı
        "answers.transport.toString()",        // metot çağrısı (izinli değişken üzerinden bile)
        "new('java.io.File', '/etc/passwd')",  // nesne oluşturma
        "while (true) { }",                    // döngü
        "x = 1",                               // atama (yan etki)
        "answers.transport = 'car'",           // cevabı değiştirmeye çalışmak
        "var a = 1; a",                        // yerel değişken
        "answers.plate == 'x'",                // alanın kendisi (önceki değil)
        "answers.unknown == 1",                // olmayan alan
        "answers['trans' + 'port'] == 'car'",  // dinamik key: hangi alana bakıldığı kaydederken bilinemez
        "runtime == null",                     // tanımsız değişken
        "answers.transport ==",                // söz dizimi hatası
    })
    void rejectsUnsafeOrUnknownExpressions(String expression) {
        assertThat(rules.check("plate", expression, Set.of("transport"))).isNotEmpty();
    }

    @Test
    void evaluatesVisibility() {
        FormField plate = field("answers.transport == 'car'", List.of());
        Map<String, Object> answers = new HashMap<>();

        answers.put("transport", "car");
        assertThat(rules.isVisible(plate, ticket, answers)).isTrue();
        answers.put("transport", "metro");
        assertThat(rules.isVisible(plate, ticket, answers)).isFalse();
    }

    @Test
    void ticketFilterAndEvaluationErrorsHideTheField() {
        assertThat(rules.isVisible(field(null, List.of(ticket)), ticket, Map.of())).isTrue();
        assertThat(rules.isVisible(field(null, List.of(ticket)), UUID.randomUUID(), Map.of())).isFalse();

        // Cevaplanmamış sayı alanıyla karşılaştırma strict modda hata verir: alan gizli sayılır.
        Map<String, Object> answers = new HashMap<>();
        answers.put("age", null);
        assertThat(rules.isVisible(field("answers.age > 18", List.of()), ticket, answers)).isFalse();
    }

    private static FormField field(String showIf, List<UUID> ticketTypeIds) {
        return new FormField("plate", "Plaka", FieldType.TEXT, true, null, showIf, ticketTypeIds);
    }
}
