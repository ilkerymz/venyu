package com.venyu.identity.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class EmailAlreadyRegisteredException extends ErrorResponseException {

    public EmailAlreadyRegisteredException() {
        super(HttpStatus.CONFLICT,
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Bu e-posta adresiyle kayıtlı bir hesap var."),
                null);
    }
}
