package com.venyu.venue.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class NotVenueOwnerException extends ErrorResponseException {

    public NotVenueOwnerException() {
        super(HttpStatus.FORBIDDEN,
                ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Bu mekan üzerinde işlem yetkin yok."),
                null);
    }
}