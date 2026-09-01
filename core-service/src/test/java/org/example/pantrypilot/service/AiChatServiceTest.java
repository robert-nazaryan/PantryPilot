package org.example.pantrypilot.service;

import java.util.List;

import org.example.pantrypilot.dto.ChatResponse;
import org.example.pantrypilot.model.ChatMessage;
import org.example.pantrypilot.model.ChatRole;
import org.example.pantrypilot.model.ChatSession;
import org.example.pantrypilot.model.User;
import org.example.pantrypilot.service.ai.AiChatTurn;
import org.example.pantrypilot.service.ai.AiProvider;
import org.example.pantrypilot.service.ai.UserContextSnapshotBuilder;
import org.example.pantrypilot.service.exception.AiUnavailableException;
import org.example.pantrypilot.service.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    private static final Long USER_ID = 42L;
    private static final Long SESSION_ID = 7L;
    private static final Long OTHER_USER_ID = 99L;

    @Mock private AiProvider aiProvider;
    @Mock private ChatSessionService sessionService;
    @Mock private UserContextSnapshotBuilder snapshotBuilder;

    private AiChatService service;

    @BeforeEach
    void setUp() {
        service = new AiChatService(aiProvider, sessionService, snapshotBuilder);
    }

    @Test
    void chat_whenNewSession_buildsSnapshotAndPersistsBothMessages() {
        when(aiProvider.isAvailable()).thenReturn(true);
        ChatSession session = ChatSession.builder().id(SESSION_ID)
                .user(User.builder().id(USER_ID).build()).build();
        when(sessionService.createFor(USER_ID)).thenReturn(session);
        when(snapshotBuilder.buildFor(USER_ID)).thenReturn("SNAPSHOT");
        when(aiProvider.chat(eq("SNAPSHOT"), eq(List.of()), eq("What can I cook?")))
                .thenReturn("Try a stir-fry.");

        ChatResponse resp = service.chat(USER_ID, null, "What can I cook?");

        assertThat(resp.sessionId()).isEqualTo(SESSION_ID);
        assertThat(resp.reply()).isEqualTo("Try a stir-fry.");
        verify(sessionService).appendMessage(session, ChatRole.USER, "What can I cook?");
        verify(sessionService).appendMessage(session, ChatRole.ASSISTANT, "Try a stir-fry.");
    }

    @Test
    void chat_whenExistingSession_reusesHistoryAndSkipsSnapshotRebuild() {
        when(aiProvider.isAvailable()).thenReturn(true);
        ChatSession session = ChatSession.builder().id(SESSION_ID)
                .user(User.builder().id(USER_ID).build()).build();
        session.getMessages().add(ChatMessage.builder()
                .session(session).role(ChatRole.USER).content("prior user msg").build());
        session.getMessages().add(ChatMessage.builder()
                .session(session).role(ChatRole.ASSISTANT).content("prior assistant reply").build());
        when(sessionService.loadOwnedWithMessages(USER_ID, SESSION_ID)).thenReturn(session);

        ArgumentCaptor<List<AiChatTurn>> historyCap = ArgumentCaptor.forClass(List.class);
        when(aiProvider.chat(isNull(), historyCap.capture(), eq("follow-up")))
                .thenReturn("continued reply");

        ChatResponse resp = service.chat(USER_ID, SESSION_ID, "follow-up");

        assertThat(resp.sessionId()).isEqualTo(SESSION_ID);
        assertThat(resp.reply()).isEqualTo("continued reply");
        List<AiChatTurn> passed = historyCap.getValue();
        assertThat(passed).hasSize(2);
        assertThat(passed.get(0).content()).isEqualTo("prior user msg");
        assertThat(passed.get(1).content()).isEqualTo("prior assistant reply");
        verify(snapshotBuilder, never()).buildFor(any());
    }

    @Test
    void chat_whenProviderUnavailable_throwsAiUnavailableAndDoesNotTouchSession() {
        when(aiProvider.isAvailable()).thenReturn(false);

        assertThatThrownBy(() -> service.chat(USER_ID, null, "hi"))
                .isInstanceOf(AiUnavailableException.class);
        verify(sessionService, never()).createFor(any());
        verify(sessionService, never()).loadOwnedWithMessages(any(), any());
        verify(aiProvider, never()).chat(any(), anyList(), any());
    }

    @Test
    void chat_whenSessionOwnedByAnotherUser_propagatesNotFound() {
        when(aiProvider.isAvailable()).thenReturn(true);
        when(sessionService.loadOwnedWithMessages(OTHER_USER_ID, SESSION_ID))
                .thenThrow(new NotFoundException("Chat session not found"));

        assertThatThrownBy(() -> service.chat(OTHER_USER_ID, SESSION_ID, "peek"))
                .isInstanceOf(NotFoundException.class);
        verify(aiProvider, never()).chat(any(), anyList(), any());
    }
}
