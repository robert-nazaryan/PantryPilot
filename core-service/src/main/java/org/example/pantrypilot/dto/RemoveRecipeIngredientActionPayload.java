package org.example.pantrypilot.dto;

public record RemoveRecipeIngredientActionPayload(
        Long recipeId,
        String recipeTitle,
        Long ingredientId,
        String ingredientName
) {
}
