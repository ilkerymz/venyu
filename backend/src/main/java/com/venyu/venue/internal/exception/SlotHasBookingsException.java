package com.venyu.venue.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class SlotHasBookingsException extends ErrorResponseException {

    public SlotHasBookingsException() {
        super(HttpStatus.CONFLICT,
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                        "Bu slot için rezervasyon kaydı olduğu için kapatılamaz."),
                null);
    }
}