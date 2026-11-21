package com.venyu.form.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.util.List;

// Bütün hatalar tek yanıtta: organizatör formu tek seferde düzeltebilsin. Yanıtta "errors" listesi.
public class InvalidFormException extends ErrorResponseException {

    public InvalidFormException(List<String> errors) {
        super(HttpStatus.BAD_REQUEST, problem(errors), null);
    }

    private static ProblemDetail problem(List<String> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Form şeması geçersiz.");
        problem.setProperty("errors", errors);
        return problem;
    }
}
