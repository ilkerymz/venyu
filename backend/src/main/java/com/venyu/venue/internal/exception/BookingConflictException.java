package com.venyu.venue.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class BookingConflictException extends ErrorResponseException {

    public BookingConflictException() {
        super(HttpStatus.CONFLICT,
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                        "Rezervasyon aynı anda başka bir işlemle değişti. Lütfen sayfayı yenileyip tekrar deneyin."),
                null);
    }
}