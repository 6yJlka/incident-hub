package ru.donskikh.incidenthub.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.donskikh.incidenthub.common.DomainConflictException;
import ru.donskikh.incidenthub.common.DomainNotFoundException;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainNotFoundException.class)
    public ProblemDetail handleNotFound(
            DomainNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "urn:incident-hub:problem:not-found",
                "Resource not found",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(DomainConflictException.class)
    public ProblemDetail handleConflict(
            DomainConflictException exception,
            HttpServletRequest request
    ) {
        ProblemDetail detail = problem(
                HttpStatus.CONFLICT,
                "urn:incident-hub:problem:conflict",
                "Domain conflict",
                exception.getMessage(),
                request
        );
        if (exception.getCurrentState() != null) {
            detail.setProperty("currentStatus", exception.getCurrentState());
        }
        if (exception.getRequiredState() != null) {
            detail.setProperty("requiredStatus", exception.getRequiredState());
        }
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        ProblemDetail detail = problem(
                HttpStatus.BAD_REQUEST,
                "urn:incident-hub:problem:validation",
                "Request validation failed",
                "One or more request fields are invalid",
                request
        );
        List<ValidationError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
                .toList();
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "urn:incident-hub:problem:malformed-request",
                "Malformed request body",
                "Request body could not be read",
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "urn:incident-hub:problem:invalid-parameter",
                "Invalid request parameter",
                "Parameter '%s' has an invalid value".formatted(exception.getName()),
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "urn:incident-hub:problem:bad-request",
                "Invalid request",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        LOGGER.error("Unhandled exception while processing {} {}", request.getMethod(), request.getRequestURI(), exception);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "urn:incident-hub:problem:internal-server-error",
                "Internal server error",
                "An unexpected error occurred",
                request
        );
    }

    private ProblemDetail problem(
            HttpStatus status,
            String type,
            String title,
            String detail,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(type));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    public record ValidationError(String field, String message) {
    }
}
