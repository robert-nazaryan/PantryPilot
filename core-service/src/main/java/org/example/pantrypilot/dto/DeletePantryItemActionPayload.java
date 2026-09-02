package org.example.pantrypilot.dto;

public record DeletePantryItemActionPayload(
        Long itemId,
        String name
) {
}
