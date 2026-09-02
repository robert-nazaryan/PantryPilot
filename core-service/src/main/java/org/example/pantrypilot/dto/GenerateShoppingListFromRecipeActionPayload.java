package org.example.pantrypilot.dto;

public record GenerateShoppingListFromRecipeActionPayload(
        Long recipeId,
        String recipeTitle
) {
}
