package com.smartpantry.dashboard;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartpantry.item.Category;
import com.smartpantry.item.ItemQueryService;
import com.smartpantry.item.ItemView;

/**
 * Builds the dashboard summary from the inventory with status (REQ-014 to
 * REQ-019). Nothing is cached: every call reads the current inventory. Date
 * logic lives in ItemQueryService; this service only reads the status string.
 */
@Service
public class DashboardService {

    private final ItemQueryService itemQueryService;

    public DashboardService(ItemQueryService itemQueryService) {
        this.itemQueryService = itemQueryService;
    }

    public DashboardSummary summary() {
        List<ItemView> items = itemQueryService.findAllWithStatus();

        List<ItemView> expiringSoon = items.stream()
                .filter(item -> ItemView.STATUS_EXPIRING.equals(item.status()))
                .toList();
        List<ItemView> expired = items.stream()
                .filter(item -> ItemView.STATUS_EXPIRED.equals(item.status()))
                .toList();

        List<DashboardSummary.CategoryTotal> categories = Arrays.stream(Category.values())
                .map(category -> categoryTotal(category, items))
                .toList();

        long totalQuantity = items.stream().mapToLong(item -> item.quantity()).sum();

        return new DashboardSummary(items.size(), totalQuantity, expiringSoon, expired, categories);
    }

    private static DashboardSummary.CategoryTotal categoryTotal(Category category, List<ItemView> items) {
        List<ItemView> inCategory = items.stream()
                .filter(item -> item.category() == category)
                .toList();
        long quantity = inCategory.stream().mapToLong(item -> item.quantity()).sum();
        return new DashboardSummary.CategoryTotal(category, inCategory.size(), quantity);
    }
}
