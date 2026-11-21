package com.venyu.identity.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class EmailAlreadyVerifiedException extends ErrorResponseException {

    public EmailAlreadyVerifiedException() {
        super(HttpStatus.CONFLICT,
            ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "E-posta adresin zaten doğrulanmış."),
            null);
    }
}
