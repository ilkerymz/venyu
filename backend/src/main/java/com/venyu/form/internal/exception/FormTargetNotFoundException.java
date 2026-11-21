package com.venyu.form.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class FormTargetNotFoundException extends ErrorResponseException {

    public FormTargetNotFoundException() {
        super(HttpStatus.NOT_FOUND,
            ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Etkinlik bulunamadı."),
            null);
    }
}
