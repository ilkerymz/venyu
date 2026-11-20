package com.venyu.identity.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class TooManyOtpRequestsException extends ErrorResponseException {

    public TooManyOtpRequestsException() {
        super(HttpStatus.TOO_MANY_REQUESTS,
                ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                        "Çok fazla doğrulama kodu istendi. Lütfen birkaç dakika sonra tekrar deneyin."),
                null);
    }
}