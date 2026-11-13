package com.venyu.identity.internal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

class EmailAlreadyRegisteredException extends ErrorResponseException {

    EmailAlreadyRegisteredException() {
        super(HttpStatus.CONFLICT,
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Bu e-posta adresiyle kayıtlı bir hesap var."),
                null);
    }
}
