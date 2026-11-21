package com.venyu.registration.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class InvalidConfirmationTokenException extends ErrorResponseException {

    public InvalidConfirmationTokenException() {
        super(HttpStatus.BAD_REQUEST,
            ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Onay bağlantısı geçersiz veya kullanılmış."),
            null);
    }
}
