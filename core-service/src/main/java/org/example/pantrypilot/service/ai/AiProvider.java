package org.example.pantrypilot.service.ai;

import java.util.List;

public interface AiProvider {

    boolean isAvailable();

    AiResponse chat(String systemContext, List<AiChatTurn> history, String userMessage);
}
