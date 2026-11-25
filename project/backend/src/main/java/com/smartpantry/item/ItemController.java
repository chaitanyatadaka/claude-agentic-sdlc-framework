package com.smartpantry.item;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Item endpoints: create, list, get, update and delete. Status is computed by
 * ItemQueryService; this controller only maps requests and responses.
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;
    private final ItemQueryService itemQueryService;

    public ItemController(ItemService itemService, ItemQueryService itemQueryService) {
        this.itemService = itemService;
        this.itemQueryService = itemQueryService;
    }

    @PostMapping
    public ResponseEntity<ItemView> create(@RequestBody ItemRequest request) {
        GroceryItem saved = itemService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(itemQueryService.toView(saved));
    }

    @GetMapping
    public List<ItemView> list() {
        return itemQueryService.findAllWithStatus();
    }

    @GetMapping("/{id}")
    public ItemView get(@PathVariable Long id) {
        return itemQueryService.getView(id);
    }

    @PutMapping("/{id}")
    public ItemView update(@PathVariable Long id, @RequestBody ItemRequest request) {
        return itemQueryService.toView(itemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        itemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
