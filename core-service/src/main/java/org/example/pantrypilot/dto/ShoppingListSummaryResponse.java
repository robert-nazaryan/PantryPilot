package org.example.pantrypilot.dto;

import org.example.pantrypilot.model.ShoppingList;

import java.time.OffsetDateTime;

public record ShoppingListSummaryResponse(
        Long id,
        String name,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static ShoppingListSummaryResponse from(ShoppingList list) {
        return new ShoppingListSummaryResponse(
                list.getId(),
                list.getName(),
                list.isActive(),
                list.getCreatedAt(),
                list.getUpdatedAt()
        );
    }
}
