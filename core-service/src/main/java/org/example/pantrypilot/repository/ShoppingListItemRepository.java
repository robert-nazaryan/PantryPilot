package org.example.pantrypilot.repository;

import java.util.Optional;

import org.example.pantrypilot.model.ShoppingListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Long> {

    Optional<ShoppingListItem> findByIdAndShoppingListId(Long id, Long shoppingListId);
}
