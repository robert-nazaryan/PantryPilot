package org.example.pantrypilot.dto;

import jakarta.validation.constraints.Size;

public record CreateShoppingListRequest(
        @Size(max = 100) String name
) {
}
