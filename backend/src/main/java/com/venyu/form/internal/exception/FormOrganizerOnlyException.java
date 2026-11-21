package com.venyu.form.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class FormOrganizerOnlyException extends ErrorResponseException {

    public FormOrganizerOnlyException() {
        super(HttpStatus.FORBIDDEN,
            ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "Formu sadece etkinliğin organizatörü değiştirebilir."),
            null);
    }
}
