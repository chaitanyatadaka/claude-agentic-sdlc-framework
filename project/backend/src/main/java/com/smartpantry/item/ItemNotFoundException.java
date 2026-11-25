package com.smartpantry.item;

/**
 * Thrown when an item id does not match a stored item (DR-012, DR-024). Maps to a
 * 404 response. The id is kept for logs only; the client receives a fixed message.
 */
public class ItemNotFoundException extends RuntimeException {

    public ItemNotFoundException(Long id) {
        super("Item " + id + " was not found.");
    }
}
