package org.example.pantrypilot.service;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.model.ChatMessage;
import org.example.pantrypilot.model.ChatRole;
import org.example.pantrypilot.model.ChatSession;
import org.example.pantrypilot.model.User;
import org.example.pantrypilot.repository.ChatMessageRepository;
import org.example.pantrypilot.repository.ChatSessionRepository;
import org.example.pantrypilot.repository.UserRepository;
import org.example.pantrypilot.service.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatSession createFor(Long userId) {
        User owner = userRepository.getReferenceById(userId);
        return sessionRepository.save(ChatSession.builder().user(owner).build());
    }

    @Transactional(readOnly = true)
    public ChatSession loadOwnedWithMessages(Long userId, Long sessionId) {
        return sessionRepository.findByIdAndUserIdWithMessages(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("Chat session not found"));
    }

    @Transactional
    public ChatMessage appendMessage(ChatSession session, ChatRole role, String content) {
        ChatMessage saved = messageRepository.save(ChatMessage.builder()
                .session(session)
                .role(role)
                .content(content)
                .build());
        session.setUpdatedAt(java.time.OffsetDateTime.now());
        sessionRepository.save(session);
        return saved;
    }
}
