package org.example.pantrypilot.dto;

import java.util.List;

public record UpdateRecipeActionPayload(
        Long recipeId,
        String currentTitle,
        String newTitle,
        String instructions,
        Integer cookTimeMinutes,
        List<String> tags
) {
}
