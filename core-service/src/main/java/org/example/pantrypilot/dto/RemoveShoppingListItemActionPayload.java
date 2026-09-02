package org.example.pantrypilot.dto;

public record RemoveShoppingListItemActionPayload(
        Long listId,
        String listName,
        Long itemId,
        String itemName
) {
}
