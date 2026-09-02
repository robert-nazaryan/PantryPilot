package org.example.pantrypilot.dto;

import java.util.List;

import org.example.pantrypilot.model.ChatActionType;

public record BulkActionPayload(
        ChatActionType subActionType,
        List<Object> targets,
        String summary
) {
}
