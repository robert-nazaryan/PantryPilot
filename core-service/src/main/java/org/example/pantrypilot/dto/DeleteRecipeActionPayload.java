package org.example.pantrypilot.dto;

public record DeleteRecipeActionPayload(
        Long recipeId,
        String recipeTitle
) {
}
