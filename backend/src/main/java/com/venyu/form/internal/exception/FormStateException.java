package com.venyu.form.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class FormStateException extends ErrorResponseException {

    public FormStateException(String detail) {
        super(HttpStatus.CONFLICT, ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail), null);
    }
}
