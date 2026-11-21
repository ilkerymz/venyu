package com.venyu.event.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class NotEventOrganizerException extends ErrorResponseException {

    public NotEventOrganizerException() {
        super(HttpStatus.FORBIDDEN,
            ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Bu etkinlik üzerinde işlem yetkin yok."),
            null);
    }
}
