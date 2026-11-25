package com.smartpantry.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ItemServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);

    @Mock
    private ItemRepository repository;

    private ItemService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ZoneId zone = ZoneId.systemDefault();
        Clock clock = Clock.fixed(TODAY.atStartOfDay(zone).toInstant(), zone);
        // The real validator is used so the rule cases run through the actual rules.
        service = new ItemService(repository, new ItemValidator(clock));
    }

    private static ItemRequest request(String name, Integer quantity, Category category,
                                       LocalDate purchase, LocalDate expiry) {
        return new ItemRequest(name, quantity, category, purchase, expiry);
    }

    private Map<String, String> rejected(ItemRequest request) {
        ItemValidationException ex = assertThrows(ItemValidationException.class,
                () -> service.create(request));
        verify(repository, never()).save(any());
        return ex.getFieldErrors();
    }

    @Test
    void createSavesOnceAndReturnsTheSavedItem() {
        when(repository.save(any(GroceryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        GroceryItem saved = service.create(
                request("Apple", 3, Category.Fruits, TODAY.minusDays(2), TODAY.plusDays(10)));

        verify(repository).save(any(GroceryItem.class));
        assertEquals("Apple", saved.getName());
        assertEquals(Integer.valueOf(3), saved.getQuantity());
        assertEquals(Category.Fruits, saved.getCategory());
        assertEquals(TODAY.minusDays(2), saved.getPurchaseDate());
        assertEquals(TODAY.plusDays(10), saved.getExpiryDate());
    }

    @Test
    void createWithoutExpiryDateSavesWithNullExpiry() {
        when(repository.save(any(GroceryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        GroceryItem saved = service.create(request("Rice", 1, Category.Grains, TODAY, null));

        assertEquals(null, saved.getExpiryDate());
        verify(repository).save(any(GroceryItem.class));
    }

    @Test
    void missingNameIsRejectedAndNothingIsSaved() {
        Map<String, String> errors = rejected(request(null, 1, Category.Dairy, TODAY, null));
        assertTrue(errors.containsKey("name"));
    }

    @Test
    void blankNameIsRejectedAndNothingIsSaved() {
        Map<String, String> errors = rejected(request("  ", 1, Category.Dairy, TODAY, null));
        assertTrue(errors.containsKey("name"));
    }

    @Test
    void zeroQuantityIsRejectedAndNothingIsSaved() {
        Map<String, String> errors = rejected(request("Milk", 0, Category.Dairy, TODAY, null));
        assertTrue(errors.containsKey("quantity"));
    }

    @Test
    void negativeQuantityIsRejectedAndNothingIsSaved() {
        Map<String, String> errors = rejected(request("Milk", -4, Category.Dairy, TODAY, null));
        assertTrue(errors.containsKey("quantity"));
    }

    @Test
    void nullCategoryIsRejectedAndNothingIsSaved() {
        // Category values outside the six names cannot reach the service: they fail at binding.
        Map<String, String> errors = rejected(request("Milk", 1, null, TODAY, null));
        assertTrue(errors.containsKey("category"));
    }

    @Test
    void purchaseDateInTheFutureIsRejectedAndNothingIsSaved() {
        Map<String, String> errors = rejected(
                request("Milk", 1, Category.Dairy, TODAY.plusDays(1), null));
        assertTrue(errors.containsKey("purchaseDate"));
    }

    @Test
    void purchaseDateAfterExpiryIsRejectedAndNothingIsSaved() {
        Map<String, String> errors = rejected(
                request("Milk", 1, Category.Dairy, TODAY, TODAY.minusDays(1)));
        assertTrue(errors.containsKey("purchaseDate"));
    }

    @Test
    void findAllReturnsTheRepositoryResultInNameThenIdOrder() {
        GroceryItem item = new GroceryItem("Apple", 1, Category.Fruits, TODAY, null);
        when(repository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(java.util.List.of(item));

        assertSame(item, service.findAll().get(0));
    }

    private static final Long ID = 7L;

    private static GroceryItem stored() {
        return new GroceryItem("Apple", 3, Category.Fruits, TODAY.minusDays(2), TODAY.plusDays(10));
    }

    @Test
    void getReturnsTheStoredItem() {
        GroceryItem item = stored();
        when(repository.findById(ID)).thenReturn(Optional.of(item));

        assertSame(item, service.get(ID));
    }

    @Test
    void getOfUnknownIdThrowsNotFound() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> service.get(ID));
    }

    @Test
    void updateReplacesTheStoredFieldsAndSaves() {
        GroceryItem item = stored();
        when(repository.findById(ID)).thenReturn(Optional.of(item));
        when(repository.save(any(GroceryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        GroceryItem updated = service.update(ID,
                request("Pear", 5, Category.Dairy, TODAY, null));

        verify(repository).save(item);
        assertEquals("Pear", updated.getName());
        assertEquals(Integer.valueOf(5), updated.getQuantity());
        assertEquals(Category.Dairy, updated.getCategory());
        assertEquals(TODAY, updated.getPurchaseDate());
        assertNull(updated.getExpiryDate());
    }

    @Test
    void secondUpdateOverwritesTheFirst() {
        GroceryItem item = stored();
        when(repository.findById(ID)).thenReturn(Optional.of(item));
        when(repository.save(any(GroceryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(ID, request("Pear", 5, Category.Fruits, TODAY, null));
        GroceryItem second = service.update(ID, request("Plum", 2, Category.Fruits, TODAY, null));

        assertEquals("Plum", second.getName());
        assertEquals(Integer.valueOf(2), second.getQuantity());
    }

    @Test
    void updateOfUnknownIdThrowsNotFoundAndSavesNothing() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class,
                () -> service.update(ID, request("Pear", 5, Category.Fruits, TODAY, null)));
        verify(repository, never()).save(any());
    }

    @Test
    void updateWithBlankNameIsRejectedAndNothingIsSaved() {
        GroceryItem item = stored();
        when(repository.findById(ID)).thenReturn(Optional.of(item));

        ItemValidationException ex = assertThrows(ItemValidationException.class,
                () -> service.update(ID, request("   ", 5, Category.Fruits, TODAY, null)));

        assertTrue(ex.getFieldErrors().containsKey("name"));
        verify(repository, never()).save(any());
        assertEquals("Apple", item.getName());
    }

    @Test
    void deleteOfExistingItemCallsDelete() {
        GroceryItem item = stored();
        when(repository.findById(ID)).thenReturn(Optional.of(item));

        service.delete(ID);

        verify(repository).delete(item);
    }

    @Test
    void deleteOfUnknownIdThrowsNotFoundAndDeletesNothing() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> service.delete(ID));
        verify(repository, never()).delete(any());
    }

    @Test
    void getAfterDeleteThrowsNotFound() {
        GroceryItem item = stored();
        when(repository.findById(ID)).thenReturn(Optional.of(item), Optional.empty());

        service.delete(ID);

        assertThrows(ItemNotFoundException.class, () -> service.get(ID));
    }
}
