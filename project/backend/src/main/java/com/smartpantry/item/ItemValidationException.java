package com.smartpantry.item;

import java.util.Map;

/** Thrown when an item request breaks a validation rule. Maps to a 400 response. */
public class ItemValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public ItemValidationException(Map<String, String> fieldErrors) {
        super("Some fields are invalid.");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    /** One message per invalid field, keyed by the request field name. */
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
