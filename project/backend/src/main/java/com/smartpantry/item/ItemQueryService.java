package com.smartpantry.item;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartpantry.expiry.ExpiryClassifier;
import com.smartpantry.expiry.ExpiryStatus;

/**
 * Read side of the inventory. Every item it returns carries the status computed
 * here, through the shared expiry classifier. Classification happens in this
 * service only, not in the repository or the controllers.
 */
@Service
public class ItemQueryService {

    private static final Sort ORDER = Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));

    private final ItemRepository repository;
    private final ExpiryClassifier classifier;

    public ItemQueryService(ItemRepository repository, ExpiryClassifier classifier) {
        this.repository = repository;
        this.classifier = classifier;
    }

    /** Every stored item with its status, ordered by name then id. */
    @Transactional(readOnly = true)
    public List<ItemView> findAllWithStatus() {
        return repository.findAll(ORDER).stream().map(this::toView).toList();
    }

    /** One item with its status. Throws ItemNotFoundException when the id is unknown. */
    @Transactional(readOnly = true)
    public ItemView getView(Long id) {
        GroceryItem item = repository.findById(id).orElseThrow(() -> new ItemNotFoundException(id));
        return toView(item);
    }

    /** Converts a stored item to its view with the status computed now. */
    public ItemView toView(GroceryItem item) {
        return new ItemView(
                item.getId(),
                item.getName(),
                item.getQuantity(),
                item.getCategory(),
                item.getPurchaseDate(),
                item.getExpiryDate(),
                statusOf(item.getExpiryDate()));
    }

    private String statusOf(LocalDate expiryDate) {
        if (expiryDate == null) {
            return ItemView.STATUS_NONE;
        }
        ExpiryStatus status = classifier.classify(expiryDate);
        return switch (status) {
            case EXPIRED -> ItemView.STATUS_EXPIRED;
            case EXPIRING_SOON -> ItemView.STATUS_EXPIRING;
            case VALID -> ItemView.STATUS_VALID;
        };
    }
}
