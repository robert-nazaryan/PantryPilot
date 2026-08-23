package org.example.pantrypilot.repository;

import java.util.List;

import org.example.pantrypilot.model.ShoppingListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Long> {

    List<ShoppingListItem> findByShoppingListId(Long shoppingListId);
}
