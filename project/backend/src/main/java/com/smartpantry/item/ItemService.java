package com.smartpantry.item;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single writer for grocery items (architecture). Validates the request
 * before anything is saved. Only create and list are implemented here.
 */
@Service
public class ItemService {

    private static final Sort ORDER = Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));

    private final ItemRepository repository;
    private final ItemValidator validator;

    public ItemService(ItemRepository repository, ItemValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    @Transactional
    public GroceryItem create(ItemRequest request) {
        Map<String, String> errors = validator.validate(request);
        if (!errors.isEmpty()) {
            throw new ItemValidationException(errors);
        }
        GroceryItem item = new GroceryItem(
                request.name(),
                request.quantity(),
                request.category(),
                request.purchaseDate(),
                request.expiryDate());
        return repository.save(item);
    }

    @Transactional(readOnly = true)
    public List<GroceryItem> findAll() {
        return repository.findAll(ORDER);
    }

    @Transactional(readOnly = true)
    public GroceryItem get(Long id) {
        return find(id);
    }

    /** Replaces the stored fields with the request. Same rules as create; last write wins. */
    @Transactional
    public GroceryItem update(Long id, ItemRequest request) {
        GroceryItem item = find(id);
        Map<String, String> errors = validator.validate(request);
        if (!errors.isEmpty()) {
            throw new ItemValidationException(errors);
        }
        item.setName(request.name());
        item.setQuantity(request.quantity());
        item.setCategory(request.category());
        item.setPurchaseDate(request.purchaseDate());
        item.setExpiryDate(request.expiryDate());
        return repository.save(item);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private GroceryItem find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ItemNotFoundException(id));
    }
}
