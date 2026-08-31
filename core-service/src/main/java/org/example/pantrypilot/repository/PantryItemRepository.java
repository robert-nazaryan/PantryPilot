package org.example.pantrypilot.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.example.pantrypilot.model.PantryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PantryItemRepository extends JpaRepository<PantryItem, Long> {

    Optional<PantryItem> findByIdAndUserId(Long id, Long userId);

    @Query(value = "SELECT p FROM PantryItem p WHERE p.user.id = :userId "
            + "ORDER BY p.expiryDate ASC NULLS LAST, p.id ASC",
            countQuery = "SELECT COUNT(p) FROM PantryItem p WHERE p.user.id = :userId")
    Page<PantryItem> findByUserIdOrderByExpiryDateAscNullsLast(
            @Param("userId") Long userId, Pageable pageable);

    List<PantryItem> findByUserIdAndExpiryDateBetweenOrderByExpiryDateAsc(
            Long userId, LocalDate start, LocalDate end);

    List<PantryItem> findByUserIdAndExpiryDateLessThanEqualOrderByExpiryDateAsc(
            Long userId, LocalDate end);

    @Query("SELECT p FROM PantryItem p "
            + "JOIN FETCH p.user u "
            + "WHERE p.expiryDate IS NOT NULL AND p.expiryDate <= :end "
            + "ORDER BY u.id ASC, p.expiryDate ASC")
    List<PantryItem> findAllExpiringByOwner(@Param("end") LocalDate end);
}
