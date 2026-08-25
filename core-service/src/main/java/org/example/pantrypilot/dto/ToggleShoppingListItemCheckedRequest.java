package org.example.pantrypilot.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleShoppingListItemCheckedRequest(
        @NotNull Boolean checked
) {
}
