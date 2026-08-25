package org.example.pantrypilot.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateShoppingListRequest(
        @Size(max = 100) String name,
        @NotNull Boolean active
) {
}
