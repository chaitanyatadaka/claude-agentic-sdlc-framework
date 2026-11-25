package com.smartpantry.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ItemValidatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);

    private static Clock fixedClock(LocalDate day) {
        ZoneId zone = ZoneId.systemDefault();
        return Clock.fixed(day.atStartOfDay(zone).toInstant(), zone);
    }

    private final ItemValidator validator = new ItemValidator(fixedClock(TODAY));

    private static ItemRequest request(String name, Integer quantity, Category category,
                                       LocalDate purchase, LocalDate expiry) {
        return new ItemRequest(name, quantity, category, purchase, expiry);
    }

    private ItemRequest valid() {
        return request("Milk", 2, Category.Dairy, TODAY.minusDays(1), TODAY.plusDays(7));
    }

    @Test
    void fullyPopulatedItemPasses() {
        assertTrue(validator.validate(valid()).isEmpty());
    }

    @Test
    void itemWithoutExpiryDatePasses() {
        assertTrue(validator.validate(request("Rice", 1, Category.Grains, TODAY, null)).isEmpty());
    }

    @Test
    void purchaseDateEqualToTodayPasses() {
        assertTrue(validator.validate(request("Rice", 1, Category.Grains, TODAY, null)).isEmpty());
    }

    @Test
    void purchaseDateEqualToExpiryDatePasses() {
        assertTrue(validator.validate(request("Rice", 1, Category.Grains, TODAY, TODAY)).isEmpty());
    }

    @Test
    void duplicateNamePasses() {
        assertTrue(validator.validate(valid()).isEmpty());
        assertTrue(validator.validate(valid()).isEmpty());
    }

    @Test
    void nullEmptyAndWhitespaceNameEachGiveANameMessage() {
        for (String name : new String[] {null, "", "   "}) {
            Map<String, String> errors = validator.validate(
                    request(name, 1, Category.Dairy, TODAY, null));
            assertTrue(errors.containsKey("name"), "name=" + name);
            assertEquals(1, errors.size());
        }
    }

    @Test
    void nonPositiveOrMissingQuantityGivesQuantityMessage() {
        for (Integer quantity : new Integer[] {null, 0, -1}) {
            Map<String, String> errors = validator.validate(
                    request("Milk", quantity, Category.Dairy, TODAY, null));
            assertTrue(errors.containsKey("quantity"), "quantity=" + quantity);
            assertEquals(1, errors.size());
        }
    }

    @Test
    void nullCategoryGivesCategoryMessage() {
        Map<String, String> errors = validator.validate(request("Milk", 1, null, TODAY, null));
        assertEquals(Map.of("category", errors.get("category")), errors);
        assertTrue(errors.get("category").contains("Vegetables"));
    }

    @Test
    void purchaseDateTomorrowGivesPurchaseDateMessage() {
        Map<String, String> errors = validator.validate(
                request("Milk", 1, Category.Dairy, TODAY.plusDays(1), null));
        assertEquals(1, errors.size());
        assertTrue(errors.containsKey("purchaseDate"));
    }

    @Test
    void purchaseDateAfterExpiryDateGivesPurchaseDateMessage() {
        Map<String, String> errors = validator.validate(
                request("Milk", 1, Category.Dairy, TODAY, TODAY.minusDays(1)));
        assertEquals(1, errors.size());
        assertTrue(errors.containsKey("purchaseDate"));
    }

    @Test
    void missingPurchaseDateGivesPurchaseDateMessage() {
        Map<String, String> errors = validator.validate(request("Milk", 1, Category.Dairy, null, null));
        assertEquals(1, errors.size());
        assertTrue(errors.containsKey("purchaseDate"));
    }

    @Test
    void severalInvalidFieldsGiveSeveralMessages() {
        Map<String, String> errors = validator.validate(
                request(" ", 0, null, TODAY.plusDays(2), TODAY));
        assertEquals(4, errors.size());
        assertTrue(errors.keySet().containsAll(
                Set.of("name", "quantity", "category", "purchaseDate")));
    }
}
