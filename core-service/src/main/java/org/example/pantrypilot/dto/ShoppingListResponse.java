package org.example.pantrypilot.dto;

import org.example.pantrypilot.model.ShoppingList;

import java.time.OffsetDateTime;
import java.util.List;

public record ShoppingListResponse(
        Long id,
        String name,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<ShoppingListItemResponse> items
) {

    public static ShoppingListResponse from(ShoppingList list) {
        List<ShoppingListItemResponse> mappedItems = list.getItems().stream()
                .map(ShoppingListItemResponse::from)
                .toList();
        return new ShoppingListResponse(
                list.getId(),
                list.getName(),
                list.isActive(),
                list.getCreatedAt(),
                list.getUpdatedAt(),
                mappedItems
        );
    }
}
