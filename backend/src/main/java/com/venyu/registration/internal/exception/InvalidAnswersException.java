package com.venyu.registration.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.util.List;

// Form cevap hataları tek yanıtta, "errors" listesiyle (form modülündeki InvalidFormException ile aynı biçim).
public class InvalidAnswersException extends ErrorResponseException {

    public InvalidAnswersException(List<String> errors) {
        super(HttpStatus.BAD_REQUEST, problem(errors), null);
    }

    private static ProblemDetail problem(List<String> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Form cevapları geçersiz.");
        problem.setProperty("errors", errors);
        return problem;
    }
}
