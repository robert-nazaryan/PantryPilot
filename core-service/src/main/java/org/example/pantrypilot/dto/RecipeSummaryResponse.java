package org.example.pantrypilot.dto;

import java.time.OffsetDateTime;

import org.example.pantrypilot.model.Recipe;

public record RecipeSummaryResponse(
        Long id,
        String title,
        Integer cookTimeMinutes,
        String[] tags,
        OffsetDateTime createdAt
) {

    public static RecipeSummaryResponse from(Recipe recipe) {
        return new RecipeSummaryResponse(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getCookTimeMinutes(),
                recipe.getTags(),
                recipe.getCreatedAt()
        );
    }
}
