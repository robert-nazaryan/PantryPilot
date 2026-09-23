package org.example.pantrypilot.dto;

public record DeleteShoppingListActionPayload(
        Long listId,
        String listName,
        int itemCount
) {
}
