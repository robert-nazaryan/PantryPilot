package org.example.pantrypilot.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.dto.ChatResponse;
import org.example.pantrypilot.model.ChatMessage;
import org.example.pantrypilot.model.ChatRole;
import org.example.pantrypilot.model.ChatSession;
import org.example.pantrypilot.service.ai.AiChatTurn;
import org.example.pantrypilot.service.ai.AiProvider;
import org.example.pantrypilot.service.ai.UserContextSnapshotBuilder;
import org.example.pantrypilot.service.exception.AiUnavailableException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AiProvider aiProvider;
    private final ChatSessionService sessionService;
    private final UserContextSnapshotBuilder snapshotBuilder;

    public ChatResponse chat(Long userId, Long sessionId, String userMessage) {
        if (!aiProvider.isAvailable()) {
            throw new AiUnavailableException("AI chat is not available");
        }

        boolean isNewSession = sessionId == null;
        ChatSession session = isNewSession
                ? sessionService.createFor(userId)
                : sessionService.loadOwnedWithMessages(userId, sessionId);

        String systemContext = isNewSession ? snapshotBuilder.buildFor(userId) : null;
        List<AiChatTurn> history = isNewSession ? List.of() : toHistory(session.getMessages());

        String reply = aiProvider.chat(systemContext, history, userMessage);

        sessionService.appendMessage(session, ChatRole.USER, userMessage);
        sessionService.appendMessage(session, ChatRole.ASSISTANT, reply);

        return new ChatResponse(session.getId(), reply);
    }

    private static List<AiChatTurn> toHistory(List<ChatMessage> messages) {
        return messages.stream()
                .map(m -> new AiChatTurn(m.getRole(), m.getContent()))
                .toList();
    }
}
