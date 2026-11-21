package com.venyu.form;

import java.util.List;
import java.util.Map;

/**
 * EventForms.validate sonucu. Çağıran her durumu ele almak zorunda (sealed + switch).
 */
public sealed interface FormValidation {

    /**
     * @param formVersion cevapların doğrulandığı versiyon; etkinliğin formu yoksa null.
     * @param answers     sadece görünen alanların normalize edilmiş cevapları (saklanacak hali).
     */
    record Valid(Integer formVersion, Map<String, Object> answers) implements FormValidation {
    }

    record Invalid(List<String> errors) implements FormValidation {
    }
}
