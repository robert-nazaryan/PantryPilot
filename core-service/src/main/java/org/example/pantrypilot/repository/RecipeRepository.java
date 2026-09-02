package org.example.pantrypilot.repository;

import java.util.List;
import java.util.Optional;

import org.example.pantrypilot.model.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    Optional<Recipe> findByIdAndUserId(Long id, Long userId);

    Page<Recipe> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT r FROM Recipe r LEFT JOIN FETCH r.ingredients "
            + "WHERE r.id = :id AND r.user.id = :userId")
    Optional<Recipe> findByIdAndUserIdWithIngredients(
            @Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT r FROM Recipe r WHERE r.user.id = :userId "
            + "AND LOWER(r.title) = LOWER(:title) ORDER BY r.id ASC")
    List<Recipe> findByUserIdAndTitleIgnoreCase(
            @Param("userId") Long userId, @Param("title") String title);
}
