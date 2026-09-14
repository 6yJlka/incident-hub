package ru.donskikh.incidenthub.common.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public final class AuthorizationProblemDetails {

    public static final URI TYPE = URI.create("urn:incident-hub:problem:access-denied");
    public static final String TITLE = "Access denied";
    public static final String DETAIL = "You do not have permission to perform this action";

    private AuthorizationProblemDetails() {
    }

    public static ProblemDetail create(String requestUri) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, DETAIL);
        problem.setType(TYPE);
        problem.setTitle(TITLE);
        problem.setInstance(URI.create(requestUri));
        return problem;
    }
}
