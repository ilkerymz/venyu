package com.venyu.venue.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class SlotOverlapException extends ErrorResponseException {

    public SlotOverlapException() {
        super(HttpStatus.CONFLICT,
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                        "Bu zaman aralığı mekanın başka bir slot'uyla çakışıyor."),
                null);
    }
}