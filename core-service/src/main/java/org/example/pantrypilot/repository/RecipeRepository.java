package org.example.pantrypilot.repository;

import java.util.List;

import org.example.pantrypilot.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    List<Recipe> findByUserId(Long userId);
}
