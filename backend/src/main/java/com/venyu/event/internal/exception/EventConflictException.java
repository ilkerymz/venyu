package com.venyu.event.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class EventConflictException extends ErrorResponseException {

    public EventConflictException() {
        super(HttpStatus.CONFLICT,
            ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Etkinlik aynı anda başka bir işlemle değişti. Lütfen sayfayı yenileyip tekrar deneyin."),
            null);
    }
}
