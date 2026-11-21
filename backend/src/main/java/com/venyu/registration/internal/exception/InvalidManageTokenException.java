package com.venyu.registration.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class InvalidManageTokenException extends ErrorResponseException {

    public InvalidManageTokenException() {
        super(HttpStatus.BAD_REQUEST,
            ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Kayıt bağlantısı geçersiz."),
            null);
    }
}
