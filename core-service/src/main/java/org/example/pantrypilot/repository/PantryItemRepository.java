package org.example.pantrypilot.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.example.pantrypilot.model.PantryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PantryItemRepository extends JpaRepository<PantryItem, Long> {

    Optional<PantryItem> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT p FROM PantryItem p WHERE p.user.id = :userId "
            + "ORDER BY p.expiryDate ASC NULLS LAST, p.id ASC")
    List<PantryItem> findByUserIdOrderByExpiryDateAscNullsLast(@Param("userId") Long userId);

    List<PantryItem> findByUserIdAndExpiryDateBetweenOrderByExpiryDateAsc(
            Long userId, LocalDate start, LocalDate end);
}
