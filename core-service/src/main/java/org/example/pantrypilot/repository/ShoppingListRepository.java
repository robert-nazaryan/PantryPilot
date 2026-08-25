package org.example.pantrypilot.repository;

import java.util.Optional;

import org.example.pantrypilot.model.ShoppingList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {

    Optional<ShoppingList> findByIdAndUserId(Long id, Long userId);

    Page<ShoppingList> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT s FROM ShoppingList s LEFT JOIN FETCH s.items "
            + "WHERE s.id = :id AND s.user.id = :userId")
    Optional<ShoppingList> findByIdAndUserIdWithItems(
            @Param("id") Long id, @Param("userId") Long userId);
}
