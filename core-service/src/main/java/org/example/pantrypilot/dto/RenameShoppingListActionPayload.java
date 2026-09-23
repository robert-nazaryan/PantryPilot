package org.example.pantrypilot.dto;

public record RenameShoppingListActionPayload(
        Long listId,
        String currentName,
        String newName
) {
}
