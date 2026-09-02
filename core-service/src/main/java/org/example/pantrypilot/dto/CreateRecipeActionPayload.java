package org.example.pantrypilot.dto;

import java.util.List;

public record CreateRecipeActionPayload(
        String title,
        String instructions,
        Integer cookTimeMinutes,
        List<String> tags
) {
}
