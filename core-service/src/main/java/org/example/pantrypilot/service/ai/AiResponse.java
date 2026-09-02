package org.example.pantrypilot.service.ai;

public record AiResponse(
        String text,
        AiFunctionCall functionCall
) {

    public boolean hasFunctionCall() {
        return functionCall != null;
    }
}
