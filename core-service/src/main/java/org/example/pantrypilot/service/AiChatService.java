package org.example.pantrypilot.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.pantrypilot.dto.ChatResponse;
import org.example.pantrypilot.model.ChatMessage;
import org.example.pantrypilot.model.ChatRole;
import org.example.pantrypilot.model.ChatSession;
import org.example.pantrypilot.service.ai.AiChatTurn;
import org.example.pantrypilot.service.ai.AiProvider;
import org.example.pantrypilot.service.ai.AiResponse;
import org.example.pantrypilot.service.ai.ChatActionProposer;
import org.example.pantrypilot.service.ai.UserContextSnapshotBuilder;
import org.example.pantrypilot.service.exception.AiUnavailableException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AiProvider aiProvider;
    private final ChatSessionService sessionService;
    private final ChatActionProposer actionProposer;
    private final UserContextSnapshotBuilder snapshotBuilder;

    public ChatResponse chat(Long userId, Long sessionId, String userMessage) {
        if (!aiProvider.isAvailable()) {
            throw new AiUnavailableException("AI chat is not available");
        }

        boolean isNewSession = sessionId == null;
        ChatSession session = isNewSession
                ? sessionService.createFor(userId)
                : sessionService.loadOwnedWithMessages(userId, sessionId);

        String systemContext = snapshotBuilder.buildFor(userId);
        List<AiChatTurn> history = isNewSession ? List.of() : toHistory(session.getMessages());

        AiResponse aiResponse = aiProvider.chat(systemContext, history, userMessage);

        ChatActionProposer.Outcome outcome = aiResponse.hasFunctionCall()
                ? actionProposer.propose(userId, session, aiResponse.functionCall())
                : ChatActionProposer.Outcome.none();

        sessionService.appendMessage(session, ChatRole.USER, userMessage);
        String assistantReply = deriveAssistantReply(aiResponse, outcome);
        sessionService.appendMessage(session, ChatRole.ASSISTANT, assistantReply);

        return new ChatResponse(session.getId(), assistantReply, outcome.proposedAction());
    }

    private static String deriveAssistantReply(AiResponse aiResponse, ChatActionProposer.Outcome outcome) {
        if (outcome.clarificationText() != null) {
            return outcome.clarificationText();
        }
        if (aiResponse.text() != null && !aiResponse.text().isBlank()) {
            return aiResponse.text();
        }
        if (outcome.proposedAction() != null) {
            return "Proposed an action — confirm to apply.";
        }
        return "";
    }

    private static List<AiChatTurn> toHistory(List<ChatMessage> messages) {
        return messages.stream()
                .map(m -> new AiChatTurn(m.getRole(), m.getContent()))
                .toList();
    }
}
