package com.venyu.registration.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class EventOrganizerOnlyException extends ErrorResponseException {

    public EventOrganizerOnlyException() {
        super(HttpStatus.FORBIDDEN,
            ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "Bu işlemi sadece etkinliğin organizatörü yapabilir."),
            null);
    }
}
