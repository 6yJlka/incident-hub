package ru.donskikh.incidenthub.common.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public final class AuthenticationProblemDetails {

    public static final URI TYPE = URI.create("urn:incident-hub:problem:authentication-required");
    public static final String TITLE = "Authentication required";
    public static final String DETAIL = "Authentication credentials are missing or invalid";

    private AuthenticationProblemDetails() {
    }

    public static ProblemDetail create(String requestUri) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, DETAIL);
        problem.setType(TYPE);
        problem.setTitle(TITLE);
        problem.setInstance(URI.create(requestUri));
        return problem;
    }
}
