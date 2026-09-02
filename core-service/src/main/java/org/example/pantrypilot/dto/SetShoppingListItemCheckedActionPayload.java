package org.example.pantrypilot.dto;

public record SetShoppingListItemCheckedActionPayload(
        Long listId,
        String listName,
        Long itemId,
        String itemName,
        boolean checked
) {
}
