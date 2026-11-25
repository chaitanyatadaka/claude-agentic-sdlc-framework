package com.smartpantry.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Sort;

import com.smartpantry.expiry.ExpiryClassifier;

class ItemQueryServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);

    @Mock
    private ItemRepository repository;

    private ItemQueryService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ZoneId zone = ZoneId.systemDefault();
        Clock clock = Clock.fixed(TODAY.atStartOfDay(zone).toInstant(), zone);
        // The real classifier is used so the status cases run through the actual bounds.
        service = new ItemQueryService(repository, new ExpiryClassifier(clock));
    }

    private static GroceryItem item(Long id, String name, LocalDate expiry) {
        GroceryItem item = new GroceryItem(name, 2, Category.Dairy, TODAY.minusDays(5), expiry);
        setId(item, id);
        return item;
    }

    private static void setId(GroceryItem item, Long id) {
        try {
            java.lang.reflect.Field field = GroceryItem.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(item, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private String statusFor(LocalDate expiry) {
        when(repository.findById(1L)).thenReturn(Optional.of(item(1L, "Test", expiry)));
        return service.getView(1L).status();
    }

    @Test
    void statusIsValidWhenExpiryIsFourOrMoreDaysAway() {
        assertEquals("valid", statusFor(TODAY.plusDays(4)));
    }

    @Test
    void statusIsExpiringWhenExpiryIsWithinThreeDays() {
        assertEquals("expiring", statusFor(TODAY.plusDays(3)));
    }

    @Test
    void statusIsExpiringWhenExpiryIsToday() {
        assertEquals("expiring", statusFor(TODAY));
    }

    @Test
    void statusIsExpiredWhenExpiryIsBeforeToday() {
        assertEquals("expired", statusFor(TODAY.minusDays(1)));
    }

    @Test
    void statusIsNoneWhenThereIsNoExpiryDate() {
        when(repository.findById(1L)).thenReturn(Optional.of(item(1L, "Rice", null)));

        ItemView view = service.getView(1L);

        assertEquals("none", view.status());
        assertNull(view.expiryDate());
    }

    @Test
    void getViewReturnsTheItemFieldsWithStatus() {
        when(repository.findById(7L)).thenReturn(Optional.of(item(7L, "Milk", TODAY.plusDays(10))));

        ItemView view = service.getView(7L);

        assertEquals(7L, view.id());
        assertEquals("Milk", view.name());
        assertEquals(2, view.quantity());
        assertEquals(Category.Dairy, view.category());
        assertEquals(TODAY.minusDays(5), view.purchaseDate());
        assertEquals(TODAY.plusDays(10), view.expiryDate());
        assertEquals("valid", view.status());
    }

    @Test
    void getViewOfUnknownIdThrowsNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> service.getView(999L));
    }

    @Test
    void findAllWithStatusReturnsEveryItemWithItsStatusInRepositoryOrder() {
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(
                item(1L, "Apple", TODAY.minusDays(1)),
                item(2L, "Bread", TODAY),
                item(3L, "Cheese", TODAY.plusDays(20)),
                item(4L, "Rice", null)));

        List<ItemView> views = service.findAllWithStatus();

        assertEquals(4, views.size());
        assertEquals(List.of("expired", "expiring", "valid", "none"),
                views.stream().map(ItemView::status).toList());
        assertEquals(List.of("Apple", "Bread", "Cheese", "Rice"),
                views.stream().map(ItemView::name).toList());
    }

    @Test
    void findAllWithStatusOfEmptyInventoryIsEmpty() {
        when(repository.findAll(any(Sort.class))).thenReturn(List.of());

        assertEquals(List.of(), service.findAllWithStatus());
    }
}
