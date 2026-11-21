package com.venyu.registration.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.time.Instant;

// Kapıdaki görevli ne zaman girildiğini görsün: yanıtta "checkedInAt".
public class AlreadyCheckedInException extends ErrorResponseException {

    public AlreadyCheckedInException(Instant checkedInAt) {
        super(HttpStatus.CONFLICT, problem(checkedInAt), null);
    }

    private static ProblemDetail problem(Instant checkedInAt) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Bu bilet zaten kullanıldı.");
        problem.setProperty("checkedInAt", checkedInAt);
        return problem;
    }
}
