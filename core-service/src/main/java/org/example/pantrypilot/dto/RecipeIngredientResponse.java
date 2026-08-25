package org.example.pantrypilot.dto;

import org.example.pantrypilot.model.RecipeIngredient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RecipeIngredientResponse(
        Long id,
        String name,
        BigDecimal quantity,
        String unit,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static RecipeIngredientResponse from(RecipeIngredient ingredient) {
        return new RecipeIngredientResponse(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getQuantity(),
                ingredient.getUnit(),
                ingredient.getCreatedAt(),
                ingredient.getUpdatedAt()
        );
    }
}
