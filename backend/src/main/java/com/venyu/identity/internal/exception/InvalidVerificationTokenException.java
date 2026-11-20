package com.venyu.identity.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class InvalidVerificationTokenException extends ErrorResponseException {

    public InvalidVerificationTokenException() {
        super(HttpStatus.BAD_REQUEST,
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                        "Doğrulama bağlantısı geçersiz veya süresi dolmuş."),
                null);
    }
}