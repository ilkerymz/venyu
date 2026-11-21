package com.venyu.registration.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class RegistrationConflictException extends ErrorResponseException {

    public RegistrationConflictException() {
        super(HttpStatus.CONFLICT,
            ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Kayıt aynı anda başka bir işlemle değişti. Lütfen tekrar deneyin."),
            null);
    }
}
