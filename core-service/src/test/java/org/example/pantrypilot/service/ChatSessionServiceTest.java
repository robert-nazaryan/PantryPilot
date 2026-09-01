package org.example.pantrypilot.service;

import java.util.Optional;

import org.example.pantrypilot.model.ChatMessage;
import org.example.pantrypilot.model.ChatRole;
import org.example.pantrypilot.model.ChatSession;
import org.example.pantrypilot.model.User;
import org.example.pantrypilot.repository.ChatMessageRepository;
import org.example.pantrypilot.repository.ChatSessionRepository;
import org.example.pantrypilot.repository.UserRepository;
import org.example.pantrypilot.service.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSessionServiceTest {

    private static final Long USER_ID = 42L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long SESSION_ID = 7L;

    @Mock private ChatSessionRepository sessionRepository;
    @Mock private ChatMessageRepository messageRepository;
    @Mock private UserRepository userRepository;

    private ChatSessionService service;

    @BeforeEach
    void setUp() {
        service = new ChatSessionService(sessionRepository, messageRepository, userRepository);
    }

    @Test
    void createFor_persistsSessionWithGivenOwner() {
        User owner = User.builder().id(USER_ID).build();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
        when(sessionRepository.save(any(ChatSession.class))).thenAnswer(inv -> {
            ChatSession in = inv.getArgument(0);
            in.setId(SESSION_ID);
            return in;
        });

        ChatSession created = service.createFor(USER_ID);

        assertThat(created.getId()).isEqualTo(SESSION_ID);
        assertThat(created.getUser()).isSameAs(owner);
    }

    @Test
    void loadOwnedWithMessages_returnsSessionWhenOwnedByUser() {
        ChatSession session = ChatSession.builder().id(SESSION_ID)
                .user(User.builder().id(USER_ID).build()).build();
        when(sessionRepository.findByIdAndUserIdWithMessages(SESSION_ID, USER_ID))
                .thenReturn(Optional.of(session));

        ChatSession loaded = service.loadOwnedWithMessages(USER_ID, SESSION_ID);

        assertThat(loaded).isSameAs(session);
    }

    @Test
    void loadOwnedWithMessages_throwsNotFoundForCrossUserAccess() {
        when(sessionRepository.findByIdAndUserIdWithMessages(SESSION_ID, OTHER_USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadOwnedWithMessages(OTHER_USER_ID, SESSION_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void appendMessage_savesMessageAndTouchesSession() {
        ChatSession session = ChatSession.builder().id(SESSION_ID)
                .user(User.builder().id(USER_ID).build()).build();
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ChatMessage saved = service.appendMessage(session, ChatRole.USER, "hello");

        assertThat(saved.getRole()).isEqualTo(ChatRole.USER);
        assertThat(saved.getContent()).isEqualTo("hello");
        assertThat(saved.getSession()).isSameAs(session);
        assertThat(session.getUpdatedAt()).isNotNull();
    }
}
