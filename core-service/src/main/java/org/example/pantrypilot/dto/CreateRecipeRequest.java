package org.example.pantrypilot.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRecipeRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String instructions,
        @Min(0) Integer cookTimeMinutes,
        String[] tags
) {}
