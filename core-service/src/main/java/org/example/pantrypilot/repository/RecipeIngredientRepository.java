package org.example.pantrypilot.repository;

import java.util.List;

import org.example.pantrypilot.model.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {

    List<RecipeIngredient> findByRecipeId(Long recipeId);
}
