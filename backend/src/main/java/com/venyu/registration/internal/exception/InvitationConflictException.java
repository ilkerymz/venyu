package com.venyu.registration.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class InvitationConflictException extends ErrorResponseException {

    public InvitationConflictException() {
        super(HttpStatus.CONFLICT,
            ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Davet aynı anda başka bir işlemle değişti. Lütfen tekrar deneyin."),
            null);
    }
}
