package com.smartpantry.item;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * Item rules for create (REQ-001, REQ-004, REQ-007, REQ-009, REQ-028 to REQ-031).
 * "Today" comes from the same Clock bean as the expiry classifier (DR-022).
 * The name is checked for blankness but stored untrimmed (A-003).
 */
@Component
public class ItemValidator {

    private final Clock clock;

    public ItemValidator(Clock clock) {
        this.clock = clock;
    }

    /** Returns one message per invalid field; an empty map means the request is valid. */
    public Map<String, String> validate(ItemRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (request.name() == null || request.name().isBlank()) {
            errors.put("name", "Name is required.");
        }

        if (request.quantity() == null) {
            errors.put("quantity", "Quantity is required.");
        } else if (request.quantity() <= 0) {
            errors.put("quantity", "Quantity must be a positive whole number.");
        }

        if (request.category() == null) {
            errors.put("category", "Category must be one of " + categoryNames() + ".");
        }

        // Purchase date is optional (REQ-001, Decided Q-001). When given, it must not be
        // later than today, and not later than the expiry date when both are given.
        LocalDate purchase = request.purchaseDate();
        if (purchase != null && purchase.isAfter(LocalDate.now(clock))) {
            errors.put("purchaseDate", "Purchase date must not be later than today.");
        } else if (purchase != null && request.expiryDate() != null && purchase.isAfter(request.expiryDate())) {
            errors.put("purchaseDate", "Purchase date must not be after the expiry date.");
        }

        return errors;
    }

    static String categoryNames() {
        return Arrays.stream(Category.values()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
