package com.venyu.form.internal.service;

import com.venyu.form.FormField;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * showIf ifadelerinin sandbox'lı JEXL motoru (ADR-009). İfadeyi organizatör değil frontend'in form builder'ı üretir,
 * ama sunucu istemciye güvenmez: yapı kaydederken kısıtlanır, değerlendirme sadece salt okunur bir context'le yapılır.
 */
@Component
class FormRules {

    static final String ANSWERS = "answers";
    static final String TICKET_TYPE_ID = "ticketTypeId";

    // Sadece ifade: atama, döngü, lambda, new, metot/fonksiyon çağrısı, yerel değişken, pragma, annotation kapalı.
    // Döngü olmadığı için her değerlendirme sınırlı sürer; ayrıca timeout gerekmez.
    private static final JexlFeatures FEATURES = new JexlFeatures()
        .sideEffect(false)
        .sideEffectGlobal(false)
        .loops(false)
        .lambda(false)
        .newInstance(false)
        .methodCall(false)
        .localVar(false)
        .pragma(false)
        .annotation(false);

    // RESTRICTED: java.lang.reflect, java.io, java.net, Runtime, System vb. sınıflara erişim yok.
    // strict: tanımsız değişken hata verir (sessizce null olmaz). cache: aynı ifade bir kez parse edilir.
    private final JexlEngine engine = new JexlBuilder()
        .features(FEATURES)
        .permissions(JexlPermissions.RESTRICTED)
        .strict(true)
        .silent(false)
        .cache(512)
        .create();

    // Kaydederken: söz dizimi ve izin verilen yapılar + sadece önceki alanlara başvuru (döngüsel bağımlılık olamaz).
    List<String> check(String fieldKey, String expression, Set<String> earlierKeys) {
        JexlScript script;
        try {
            script = engine.createScript(expression);
        } catch (JexlException e) {
            return List.of(fieldKey + ": showIf ifadesi geçersiz veya izin verilmeyen bir yapı içeriyor.");
        }
        List<String> errors = new ArrayList<>();
        for (List<String> path : script.getVariables()) {
            boolean ticket = path.equals(List.of(TICKET_TYPE_ID));
            boolean earlierAnswer = path.size() == 2 && path.get(0).equals(ANSWERS) && earlierKeys.contains(path.get(1));
            if (!ticket && !earlierAnswer) {
                errors.add(fieldKey + ": showIf sadece önceki alanlara (answers.<key>) ve ticketTypeId'ye başvurabilir, '"
                    + String.join(".", path) + "' geçersiz.");
            }
        }
        return errors;
    }

    // answers: önceki alanların (gizliyse null) normalize edilmiş cevapları.
    boolean isVisible(FormField field, UUID ticketTypeId, Map<String, Object> answers) {
        if (!field.ticketTypeIds().isEmpty() && !field.ticketTypeIds().contains(ticketTypeId)) {
            return false;
        }
        if (field.showIf() == null) {
            return true;
        }
        MapContext context = new MapContext();
        context.set(ANSWERS, answers);
        context.set(TICKET_TYPE_ID, ticketTypeId.toString());
        try {
            return Boolean.TRUE.equals(engine.createScript(field.showIf()).execute(context));
        } catch (JexlException e) {
            // Örn. cevaplanmamış sayı alanıyla karşılaştırma (strict modda null > 2): soru gösterilmez.
            return false;
        }
    }
}
