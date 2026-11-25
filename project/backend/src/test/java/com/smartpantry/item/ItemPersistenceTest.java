package com.smartpantry.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.smartpantry.SmartPantryApplication;

class ItemPersistenceTest {

    @TempDir
    Path tempDir;

    /**
     * Opens the application against the H2 file in the temporary directory.
     * The URL is passed as a command-line argument: builder default properties
     * rank below application.properties, which would point at ./data/.
     */
    private ConfigurableApplicationContext open() {
        String dbFile = tempDir.toAbsolutePath().toString().replace('\\', '/') + "/smartpantry";
        return new SpringApplicationBuilder(SmartPantryApplication.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .run(
                        "--spring.datasource.url=jdbc:h2:file:" + dbFile,
                        "--spring.jpa.hibernate.ddl-auto=update");
    }

    @Test
    void itemWithAllFieldsSurvivesClosingAndReopeningTheFile() {
        LocalDate purchase = LocalDate.of(2026, 11, 20);
        LocalDate expiry = LocalDate.of(2026, 12, 1);
        Long id;

        try (ConfigurableApplicationContext ctx = open()) {
            ItemRepository repo = ctx.getBean(ItemRepository.class);
            id = repo.save(new GroceryItem("Apple", 3, Category.Fruits, purchase, expiry)).getId();
        }

        try (ConfigurableApplicationContext ctx = open()) {
            GroceryItem found = ctx.getBean(ItemRepository.class).findById(id).orElseThrow();
            assertEquals("Apple", found.getName());
            assertEquals(Integer.valueOf(3), found.getQuantity());
            assertEquals(Category.Fruits, found.getCategory());
            assertEquals(purchase, found.getPurchaseDate());
            assertEquals(expiry, found.getExpiryDate());
        }
    }

    @Test
    void allSixCategoriesRoundTripAndAreStoredAsStrings() {
        try (ConfigurableApplicationContext ctx = open()) {
            ItemRepository repo = ctx.getBean(ItemRepository.class);
            for (Category c : Category.values()) {
                repo.save(new GroceryItem("item-" + c.name(), 1, c, null, null));
            }
        }

        try (ConfigurableApplicationContext ctx = open()) {
            ItemRepository repo = ctx.getBean(ItemRepository.class);
            Set<Category> reloaded = repo.findAll().stream()
                    .map(GroceryItem::getCategory)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(Category.class)));
            assertEquals(EnumSet.allOf(Category.class), reloaded);

            List<String> stored = ctx.getBean(JdbcTemplate.class)
                    .queryForList("SELECT category FROM grocery_item", String.class);
            assertEquals(6, stored.size());
            assertTrue(stored.containsAll(List.of("Vegetables", "Fruits", "Dairy", "Grains", "Snacks", "Others")));
        }
    }

    @Test
    void categoryEnumHoldsExactlyTheSixFixedNames() {
        List<String> names = List.of(Category.values()).stream().map(Enum::name).toList();
        assertEquals(List.of("Vegetables", "Fruits", "Dairy", "Grains", "Snacks", "Others"), names);
    }

    @Test
    void findByIdForUnknownIdReturnsEmpty() {
        try (ConfigurableApplicationContext ctx = open()) {
            ItemRepository repo = ctx.getBean(ItemRepository.class);
            assertFalse(repo.findById(999_999L).isPresent());
        }
    }

    @Test
    void nullNameFailsWithDataIntegrityViolation() {
        try (ConfigurableApplicationContext ctx = open()) {
            ItemRepository repo = ctx.getBean(ItemRepository.class);
            assertThrows(DataIntegrityViolationException.class,
                    () -> repo.saveAndFlush(new GroceryItem(null, 1, Category.Dairy, null, null)));
        }
    }

    @Test
    void nullQuantityFailsWithDataIntegrityViolation() {
        try (ConfigurableApplicationContext ctx = open()) {
            ItemRepository repo = ctx.getBean(ItemRepository.class);
            assertThrows(DataIntegrityViolationException.class,
                    () -> repo.saveAndFlush(new GroceryItem("Milk", null, Category.Dairy, null, null)));
        }
    }

    @Test
    void nullCategoryFailsWithDataIntegrityViolation() {
        try (ConfigurableApplicationContext ctx = open()) {
            ItemRepository repo = ctx.getBean(ItemRepository.class);
            assertThrows(DataIntegrityViolationException.class,
                    () -> repo.saveAndFlush(new GroceryItem("Milk", 1, null, null, null)));
        }
    }

    @Test
    void nullPurchaseAndExpiryDatesPersistAsNull() {
        Long id;
        try (ConfigurableApplicationContext ctx = open()) {
            id = ctx.getBean(ItemRepository.class)
                    .save(new GroceryItem("Rice", 2, Category.Grains, null, null)).getId();
        }

        try (ConfigurableApplicationContext ctx = open()) {
            GroceryItem found = ctx.getBean(ItemRepository.class).findById(id).orElseThrow();
            assertNull(found.getPurchaseDate());
            assertNull(found.getExpiryDate());
            assertEquals("Rice", found.getName());
        }
    }
}
