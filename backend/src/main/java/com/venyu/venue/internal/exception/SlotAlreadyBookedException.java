package com.venyu.venue.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class SlotAlreadyBookedException extends ErrorResponseException {

    public SlotAlreadyBookedException() {
        super(HttpStatus.CONFLICT,
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Bu slot şu anda başka bir rezervasyonda."),
                null);
    }
}