package com.venyu.event.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class EventStateException extends ErrorResponseException {

    public EventStateException(String detail) {
        super(HttpStatus.CONFLICT, ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail), null);
    }
}
