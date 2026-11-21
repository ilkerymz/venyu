package com.venyu.registration.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class SoldOutException extends ErrorResponseException {

    public SoldOutException() {
        super(HttpStatus.CONFLICT,
            ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Bu bilet türü tükendi."),
            null);
    }
}
