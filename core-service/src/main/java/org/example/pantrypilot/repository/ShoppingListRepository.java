package org.example.pantrypilot.repository;

import java.util.List;

import org.example.pantrypilot.model.ShoppingList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {

    List<ShoppingList> findByUserId(Long userId);
}
