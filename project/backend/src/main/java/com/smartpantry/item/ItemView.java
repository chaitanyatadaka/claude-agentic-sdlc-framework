package com.smartpantry.item;

import java.time.LocalDate;

/**
 * An item as the REST API and the other services see it, with its status string
 * computed by ItemQueryService. The status is one of "valid", "expiring"
 * (3 days or less, today included), "expired", or "none" when the item has no
 * expiry date.
 */
public record ItemView(
        Long id,
        String name,
        Integer quantity,
        Category category,
        LocalDate purchaseDate,
        LocalDate expiryDate,
        String status) {

    public static final String STATUS_VALID = "valid";
    public static final String STATUS_EXPIRING = "expiring";
    public static final String STATUS_EXPIRED = "expired";
    public static final String STATUS_NONE = "none";
}
