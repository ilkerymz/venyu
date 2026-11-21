package com.venyu.registration.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class InvalidInvitationTokenException extends ErrorResponseException {

    public InvalidInvitationTokenException() {
        super(HttpStatus.BAD_REQUEST,
            ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Davet bağlantısı geçersiz."),
            null);
    }
}
