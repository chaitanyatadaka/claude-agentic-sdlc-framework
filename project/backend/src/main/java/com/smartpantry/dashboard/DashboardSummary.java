package com.smartpantry.dashboard;

import java.util.List;

import com.smartpantry.item.Category;
import com.smartpantry.item.ItemView;

/**
 * The dashboard figures (REQ-014 to REQ-018). Totals count every stored item,
 * including items with no expiry date. The expiringSoon and expired lists hold
 * only items with those statuses. categories has one entry per category in
 * REQ-007 order, including categories with no items.
 */
public record DashboardSummary(
        int totalCount,
        long totalQuantity,
        List<ItemView> expiringSoon,
        List<ItemView> expired,
        List<CategoryTotal> categories) {

    /** Item count and quantity total for one category. */
    public record CategoryTotal(Category category, int itemCount, long quantityTotal) {
    }
}
