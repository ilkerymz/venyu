package com.venyu.identity.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class PasswordTooLongException extends ErrorResponseException {

    public PasswordTooLongException() {
        super(HttpStatus.BAD_REQUEST,
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Şifre çok uzun."),
                null);
    }
}