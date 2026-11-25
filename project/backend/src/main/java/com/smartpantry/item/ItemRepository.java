package com.smartpantry.item;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<GroceryItem, Long> {
}
