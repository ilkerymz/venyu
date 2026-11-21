package com.venyu.event.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class VenueBookingAlreadyUsedException extends ErrorResponseException {

    public VenueBookingAlreadyUsedException() {
        super(HttpStatus.CONFLICT,
            ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Bu mekan rezervasyonu zaten bir etkinliğe bağlı."),
            null);
    }
}
