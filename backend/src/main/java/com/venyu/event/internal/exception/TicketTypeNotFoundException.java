package com.venyu.event.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class TicketTypeNotFoundException extends ErrorResponseException {

    public TicketTypeNotFoundException() {
        super(HttpStatus.NOT_FOUND,
            ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Bilet türü bulunamadı."),
            null);
    }
}
