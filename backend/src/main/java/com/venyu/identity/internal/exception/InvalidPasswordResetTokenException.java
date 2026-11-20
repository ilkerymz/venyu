package com.venyu.identity.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class InvalidPasswordResetTokenException extends ErrorResponseException {

    public InvalidPasswordResetTokenException() {
        super(HttpStatus.BAD_REQUEST,
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                        "Şifre sıfırlama bağlantısı geçersiz veya süresi dolmuş."),
                null);
    }
}