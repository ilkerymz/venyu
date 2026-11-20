package com.venyu.identity.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class PhoneAlreadyInUseException extends ErrorResponseException {

    public PhoneAlreadyInUseException() {
        super(HttpStatus.CONFLICT,
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                        "Bu telefon numarası başka bir hesapta doğrulanmış."),
                null);
    }
}