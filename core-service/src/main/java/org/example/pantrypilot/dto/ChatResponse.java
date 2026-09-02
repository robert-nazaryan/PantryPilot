package org.example.pantrypilot.dto;

public record ChatResponse(
        Long sessionId,
        String reply,
        ProposedActionResponse proposedAction
) {

    public ChatResponse(Long sessionId, String reply) {
        this(sessionId, reply, null);
    }
}
