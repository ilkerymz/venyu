package com.venyu.event.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class EventNotFoundException extends ErrorResponseException {

    public EventNotFoundException() {
        super(HttpStatus.NOT_FOUND,
            ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Etkinlik bulunamadı."),
            null);
    }
}
