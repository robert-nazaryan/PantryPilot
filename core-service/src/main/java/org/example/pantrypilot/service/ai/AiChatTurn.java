package org.example.pantrypilot.service.ai;

import org.example.pantrypilot.model.ChatRole;

public record AiChatTurn(
        ChatRole role,
        String content
) {
}
