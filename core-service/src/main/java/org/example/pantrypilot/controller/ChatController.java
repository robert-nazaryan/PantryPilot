package org.example.pantrypilot.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.dto.ChatRequest;
import org.example.pantrypilot.dto.ChatResponse;
import org.example.pantrypilot.dto.ConfirmActionResponse;
import org.example.pantrypilot.security.CurrentUserId;
import org.example.pantrypilot.service.AiChatService;
import org.example.pantrypilot.service.ChatActionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class ChatController {

    private final AiChatService aiChatService;
    private final ChatActionService chatActionService;

    @PostMapping("/chat")
    public ChatResponse chat(
            @CurrentUserId Long userId,
            @Valid @RequestBody ChatRequest request) {
        return aiChatService.chat(userId, request.sessionId(), request.message());
    }

    @PostMapping("/chat/actions/{actionId}/confirm")
    public ConfirmActionResponse confirmAction(
            @CurrentUserId Long userId,
            @PathVariable Long actionId) {
        return chatActionService.confirm(userId, actionId);
    }
}
