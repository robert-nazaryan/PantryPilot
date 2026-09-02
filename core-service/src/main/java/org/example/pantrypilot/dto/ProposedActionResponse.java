package org.example.pantrypilot.dto;

import org.example.pantrypilot.model.ChatActionStatus;
import org.example.pantrypilot.model.ChatActionType;

public record ProposedActionResponse(
        Long actionId,
        ChatActionType type,
        ChatActionStatus status,
        Object payload
) {
}
