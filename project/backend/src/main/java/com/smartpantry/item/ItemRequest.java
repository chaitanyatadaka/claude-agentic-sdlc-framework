package com.smartpantry.item;

import java.time.LocalDate;

/**
 * The create request body. Types are bound by Jackson; invalid types are
 * reported by the API error handler, and field rules by ItemValidator.
 */
public record ItemRequest(
        String name,
        Integer quantity,
        Category category,
        LocalDate purchaseDate,
        LocalDate expiryDate) {
}
