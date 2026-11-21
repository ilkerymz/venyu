package com.venyu.registration.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class AlreadyRegisteredException extends ErrorResponseException {

    public AlreadyRegisteredException() {
        super(HttpStatus.CONFLICT,
            ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Bu etkinliğe zaten kayıtlısın."),
            null);
    }
}
