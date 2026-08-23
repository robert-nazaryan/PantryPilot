package org.example.pantrypilot.repository;

import java.time.LocalDate;
import java.util.List;

import org.example.pantrypilot.model.PantryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PantryItemRepository extends JpaRepository<PantryItem, Long> {

    List<PantryItem> findByUserIdOrderByExpiryDateAsc(Long userId);

    List<PantryItem> findByUserIdAndExpiryDateBetween(Long userId, LocalDate start, LocalDate end);
}
