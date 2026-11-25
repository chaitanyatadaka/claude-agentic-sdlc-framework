package com.smartpantry.api;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.smartpantry.api.CorrelationIdFilter;
import com.smartpantry.item.Category;
import com.smartpantry.item.ItemNotFoundException;
import com.smartpantry.item.ItemValidationException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Maps failures to JSON error bodies with a correlation ID (DR-011, DR-023).
 * Every handled failure is logged with the same ID. Clients never receive a
 * stack trace, SQL, class name or configuration value.
 */
@RestControllerAdvice
public class ApiErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorHandler.class);

    static final String VALIDATION_MESSAGE = "Some fields are invalid.";
    static final String NOT_FOUND_MESSAGE = "The item was not found.";
    static final String BODY_MESSAGE = "The request body is missing or is not valid JSON.";
    static final String BINDING_MESSAGE = "Some fields have invalid values.";
    static final String UNAVAILABLE_MESSAGE = "The data store is unavailable. Try again later.";
    static final String INTERNAL_MESSAGE = "Something went wrong. Try again later.";

    @ExceptionHandler(ItemValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(ItemValidationException ex,
                                                             HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, VALIDATION_MESSAGE, ex.getFieldErrors(), request, ex);
    }

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ItemNotFoundException ex,
                                                           HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE, Map.of(), request, ex);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                             HttpServletRequest request) {
        Map<String, String> fieldErrors = bindingFieldErrors(ex.getCause());
        String message = fieldErrors.isEmpty() ? BODY_MESSAGE : BINDING_MESSAGE;
        return respond(HttpStatus.BAD_REQUEST, message, fieldErrors, request, ex);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleArgumentType(MethodArgumentTypeMismatchException ex,
                                                               HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "A request parameter has an invalid value.",
                Map.of(ex.getName(), "has an invalid value"), request, ex);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDataAccess(DataAccessException ex,
                                                             HttpServletRequest request) {
        return respond(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE_MESSAGE, Map.of(), request, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleOther(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse framework) {
            // Spring MVC's own 4xx responses (unknown path, wrong method) keep their status.
            HttpStatusCode status = framework.getStatusCode();
            String message = status.is4xxClientError()
                    ? "The request could not be processed."
                    : INTERNAL_MESSAGE;
            return respond(status, message, Map.of(), request, ex);
        }
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_MESSAGE, Map.of(), request, ex);
    }

    private ResponseEntity<ApiErrorResponse> respond(HttpStatusCode status, String message,
                                                     Map<String, String> fieldErrors,
                                                     HttpServletRequest request, Exception ex) {
        String id = correlationId(request);
        if (status.is5xxServerError()) {
            log.error("correlationId={} status={} failure={}", id, status.value(),
                    ex.getClass().getName(), ex);
        } else {
            log.warn("correlationId={} status={} failure={}", id, status.value(),
                    ex.getClass().getSimpleName());
        }
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(id, message, new LinkedHashMap<>(fieldErrors)));
    }

    private static String correlationId(HttpServletRequest request) {
        Object id = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return id == null ? "unknown" : id.toString();
    }

    /** Names the offending field of a request-body binding failure. Values are never echoed. */
    static Map<String, String> bindingFieldErrors(Throwable cause) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (cause instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            String field = mismatch.getPath().get(0).getFieldName();
            if (field != null) {
                errors.put(field, bindingMessage(mismatch));
            }
        }
        return errors;
    }

    private static String bindingMessage(MismatchedInputException mismatch) {
        Class<?> target = mismatch.getTargetType();
        if (mismatch instanceof InvalidFormatException && target != null) {
            if (target.isEnum()) {
                return "Must be one of " + Arrays.stream(Category.values())
                        .map(Enum::name).collect(Collectors.joining(", ")) + ".";
            }
            if (target == LocalDate.class) {
                return "Must be a date in yyyy-MM-dd format.";
            }
        }
        if (target == Integer.class || target == int.class) {
            return "Must be a whole number.";
        }
        return "Has an invalid value.";
    }
}
