package com.venyu.registration.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class TicketNotFoundException extends ErrorResponseException {

    public TicketNotFoundException() {
        super(HttpStatus.NOT_FOUND,
            ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Bu etkinlikte böyle bir bilet yok."),
            null);
    }
}
