package com.venyu.identity.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class InvalidOtpCodeException extends ErrorResponseException {

    public InvalidOtpCodeException() {
        super(HttpStatus.BAD_REQUEST,
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                        "Doğrulama kodu hatalı veya süresi dolmuş."),
                null);
    }
}