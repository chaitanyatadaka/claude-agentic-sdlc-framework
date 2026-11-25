package com.smartpantry.api;

import java.util.Map;

/**
 * The JSON error body. Carries no stack trace, SQL, class name or configuration value.
 * fieldErrors is empty when the failure is not tied to one field.
 */
public record ApiErrorResponse(
        String correlationId,
        String message,
        Map<String, String> fieldErrors) {
}
