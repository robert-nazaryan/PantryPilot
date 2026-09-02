package org.example.pantrypilot.dto;

import org.example.pantrypilot.model.ChatActionType;

public record ConfirmActionResponse(
        ChatActionType actionType,
        PantryItemResponse item
) {
}
