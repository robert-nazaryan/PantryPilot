package org.example.pantrypilot.dto;

import org.example.pantrypilot.model.Recipe;

import java.time.OffsetDateTime;
import java.util.List;

public record RecipeResponse(
        Long id,
        String title,
        String instructions,
        Integer cookTimeMinutes,
        String[] tags,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<RecipeIngredientResponse> ingredients
) {

    public static RecipeResponse from(Recipe recipe) {
        List<RecipeIngredientResponse> mappedIngredients = recipe.getIngredients().stream()
                .map(RecipeIngredientResponse::from)
                .toList();
        return new RecipeResponse(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getInstructions(),
                recipe.getCookTimeMinutes(),
                recipe.getTags(),
                recipe.getCreatedAt(),
                recipe.getUpdatedAt(),
                mappedIngredients
        );
    }
}
