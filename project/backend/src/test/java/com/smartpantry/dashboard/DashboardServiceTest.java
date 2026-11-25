package com.smartpantry.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.smartpantry.item.Category;
import com.smartpantry.item.ItemQueryService;
import com.smartpantry.item.ItemView;

class DashboardServiceTest {

    @Mock
    private ItemQueryService itemQueryService;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DashboardService(itemQueryService);
    }

    private static ItemView view(long id, String name, int quantity, Category category,
                                 LocalDate expiry, String status) {
        return new ItemView(id, name, quantity, category, LocalDate.of(2026, 11, 1), expiry, status);
    }

    private static DashboardSummary.CategoryTotal total(DashboardSummary summary, Category category) {
        return summary.categories().stream()
                .filter(t -> t.category() == category)
                .findFirst()
                .orElseThrow();
    }

    private List<ItemView> seeded() {
        return List.of(
                view(1L, "Apple", 3, Category.Fruits, LocalDate.of(2026, 11, 28), "expiring"),
                view(2L, "Bread", 1, Category.Grains, LocalDate.of(2026, 11, 20), "expired"),
                view(3L, "Cheese", 2, Category.Dairy, LocalDate.of(2026, 12, 20), "valid"),
                view(4L, "Rice", 5, Category.Grains, null, "none"),
                view(5L, "Carrot", 4, Category.Vegetables, LocalDate.of(2026, 11, 25), "expiring"));
    }

    @Test
    void totalsMatchTheSeededItems() {
        when(itemQueryService.findAllWithStatus()).thenReturn(seeded());

        DashboardSummary summary = service.summary();

        assertEquals(5, summary.totalCount());
        assertEquals(15L, summary.totalQuantity());
    }

    @Test
    void expiringSoonContainsExactlyTheExpiringItems() {
        when(itemQueryService.findAllWithStatus()).thenReturn(seeded());

        List<String> names = service.summary().expiringSoon().stream().map(ItemView::name).toList();

        assertEquals(List.of("Apple", "Carrot"), names);
    }

    @Test
    void expiredContainsExactlyTheExpiredItems() {
        when(itemQueryService.findAllWithStatus()).thenReturn(seeded());

        List<String> names = service.summary().expired().stream().map(ItemView::name).toList();

        assertEquals(List.of("Bread"), names);
    }

    @Test
    void categoryTotalsSumToOverallTotals() {
        when(itemQueryService.findAllWithStatus()).thenReturn(seeded());

        DashboardSummary summary = service.summary();

        long count = summary.categories().stream().mapToLong(DashboardSummary.CategoryTotal::itemCount).sum();
        long quantity = summary.categories().stream().mapToLong(DashboardSummary.CategoryTotal::quantityTotal).sum();
        assertEquals(summary.totalCount(), count);
        assertEquals(summary.totalQuantity(), quantity);
        assertEquals(2, total(summary, Category.Grains).itemCount());
        assertEquals(6L, total(summary, Category.Grains).quantityTotal());
    }

    @Test
    void categoriesListAllSixInRequirementOrderIncludingEmptyOnes() {
        when(itemQueryService.findAllWithStatus()).thenReturn(seeded());

        DashboardSummary summary = service.summary();

        assertEquals(List.of(Category.values()),
                summary.categories().stream().map(DashboardSummary.CategoryTotal::category).toList());
        assertEquals(0, total(summary, Category.Snacks).itemCount());
        assertEquals(0L, total(summary, Category.Snacks).quantityTotal());
    }

    @Test
    void itemWithNoExpiryCountsInTotalsButNotInExpiringOrExpired() {
        when(itemQueryService.findAllWithStatus()).thenReturn(List.of(
                view(4L, "Rice", 5, Category.Grains, null, "none")));

        DashboardSummary summary = service.summary();

        assertEquals(1, summary.totalCount());
        assertEquals(5L, summary.totalQuantity());
        assertEquals(1, total(summary, Category.Grains).itemCount());
        assertTrue(summary.expiringSoon().isEmpty());
        assertTrue(summary.expired().isEmpty());
    }

    @Test
    void emptyInventoryGivesZeroTotalsAndSixEmptyCategories() {
        when(itemQueryService.findAllWithStatus()).thenReturn(List.of());

        DashboardSummary summary = service.summary();

        assertEquals(0, summary.totalCount());
        assertEquals(0L, summary.totalQuantity());
        assertTrue(summary.expiringSoon().isEmpty());
        assertTrue(summary.expired().isEmpty());
        assertEquals(6, summary.categories().size());
        assertTrue(summary.categories().stream().allMatch(t -> t.itemCount() == 0 && t.quantityTotal() == 0L));
    }

    @Test
    void secondCallReflectsAChangedInventory() {
        when(itemQueryService.findAllWithStatus())
                .thenReturn(seeded())
                .thenReturn(List.of(view(9L, "Milk", 2, Category.Dairy, LocalDate.of(2026, 11, 26), "expiring")));

        assertEquals(5, service.summary().totalCount());

        DashboardSummary second = service.summary();
        assertEquals(1, second.totalCount());
        assertEquals(2L, second.totalQuantity());
        assertEquals(List.of("Milk"), second.expiringSoon().stream().map(ItemView::name).toList());
    }
}
