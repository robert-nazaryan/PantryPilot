package org.example.pantrypilot.dto;

import java.math.BigDecimal;

public record AddRecipeIngredientActionPayload(
        Long recipeId,
        String recipeTitle,
        String name,
        BigDecimal quantity,
        String unit
) {
}
