package com.venyu.registration.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class RegistrationTargetNotFoundException extends ErrorResponseException {

    public RegistrationTargetNotFoundException() {
        super(HttpStatus.NOT_FOUND,
            ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Etkinlik veya bilet türü bulunamadı."),
            null);
    }
}
