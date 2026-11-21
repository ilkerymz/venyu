package com.venyu.identity.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class TooManyVerificationEmailsException extends ErrorResponseException {

    public TooManyVerificationEmailsException() {
        super(HttpStatus.TOO_MANY_REQUESTS,
            ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                "Doğrulama e-postasını az önce gönderdik. Bir dakika sonra tekrar deneyebilirsin."),
            null);
    }
}
