package com.venyu.identity.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class InvalidRefreshTokenException extends ErrorResponseException {

    public InvalidRefreshTokenException() {
        super(HttpStatus.UNAUTHORIZED,
                ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                        "Oturum geçersiz veya süresi dolmuş. Lütfen tekrar giriş yapın."),
                null);
    }
}