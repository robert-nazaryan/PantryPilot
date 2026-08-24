package org.example.pantrypilot.service;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.dto.CreateRecipeIngredientRequest;
import org.example.pantrypilot.dto.RecipeIngredientResponse;
import org.example.pantrypilot.dto.UpdateRecipeIngredientRequest;
import org.example.pantrypilot.model.Recipe;
import org.example.pantrypilot.model.RecipeIngredient;
import org.example.pantrypilot.repository.RecipeIngredientRepository;
import org.example.pantrypilot.repository.RecipeRepository;
import org.example.pantrypilot.service.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecipeIngredientService {

    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;

    @Transactional
    public RecipeIngredientResponse addIngredient(
            Long userId, Long recipeId, CreateRecipeIngredientRequest req) {
        Recipe recipe = loadOwnedRecipe(userId, recipeId);
        RecipeIngredient ingredient = RecipeIngredient.builder()
                .recipe(recipe)
                .name(req.name())
                .quantity(req.quantity())
                .unit(req.unit())
                .build();
        return RecipeIngredientResponse.from(recipeIngredientRepository.save(ingredient));
    }

    @Transactional
    public RecipeIngredientResponse updateIngredient(
            Long userId, Long recipeId, Long ingredientId, UpdateRecipeIngredientRequest req) {
        RecipeIngredient ingredient = loadOwnedIngredient(userId, recipeId, ingredientId);
        ingredient.setName(req.name());
        ingredient.setQuantity(req.quantity());
        ingredient.setUnit(req.unit());
        return RecipeIngredientResponse.from(recipeIngredientRepository.save(ingredient));
    }

    @Transactional
    public void deleteIngredient(Long userId, Long recipeId, Long ingredientId) {
        RecipeIngredient ingredient = loadOwnedIngredient(userId, recipeId, ingredientId);
        recipeIngredientRepository.delete(ingredient);
    }

    private Recipe loadOwnedRecipe(Long userId, Long recipeId) {
        return recipeRepository.findByIdAndUserId(recipeId, userId)
                .orElseThrow(() -> new NotFoundException("Recipe not found"));
    }

    private RecipeIngredient loadOwnedIngredient(Long userId, Long recipeId, Long ingredientId) {
        loadOwnedRecipe(userId, recipeId);
        return recipeIngredientRepository.findByIdAndRecipeId(ingredientId, recipeId)
                .orElseThrow(() -> new NotFoundException("Recipe ingredient not found"));
    }
}
