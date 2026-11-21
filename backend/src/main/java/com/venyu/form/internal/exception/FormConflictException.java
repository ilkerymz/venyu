package com.venyu.form.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class FormConflictException extends ErrorResponseException {

    public FormConflictException() {
        super(HttpStatus.CONFLICT,
            ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Form aynı anda başka bir işlemle değişti. Lütfen tekrar deneyin."),
            null);
    }
}
