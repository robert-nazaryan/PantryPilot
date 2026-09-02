package org.example.pantrypilot.service.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.ObjectMapper;
import org.example.pantrypilot.dto.ConsumePantryItemActionPayload;
import org.example.pantrypilot.dto.DeletePantryItemActionPayload;
import org.example.pantrypilot.dto.ProposedActionResponse;
import org.example.pantrypilot.dto.UpdatePantryItemActionPayload;
import org.example.pantrypilot.model.ChatAction;
import org.example.pantrypilot.model.ChatActionStatus;
import org.example.pantrypilot.model.ChatActionType;
import org.example.pantrypilot.model.ChatSession;
import org.example.pantrypilot.model.PantryItem;
import org.example.pantrypilot.model.User;
import org.example.pantrypilot.service.ChatActionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatActionProposerTest {

    private static final Long USER_ID = 42L;
    private static final Long ITEM_ID = 88L;
    private static final Long ACTION_ID = 501L;

    @Mock private ChatActionService actionService;
    @Mock private PantryItemNameResolver nameResolver;

    private ObjectMapper objectMapper;
    private ChatActionProposer proposer;
    private ChatSession session;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        proposer = new ChatActionProposer(actionService, nameResolver, objectMapper);
        session = ChatSession.builder().user(User.builder().id(USER_ID).build()).build();
    }

    @Test
    void propose_createTool_parsesArgsAndPersists() {
        AiFunctionCall call = new AiFunctionCall(GeminiProvider.TOOL_CREATE_PANTRY_ITEM, Map.of(
                "name", "Milk",
                "quantity", 2,
                "unit", "L",
                "category", "dairy",
                "expiryDate", "2099-01-01"));
        stubPersist(ChatActionType.CREATE_PANTRY_ITEM);

        ChatActionProposer.Outcome outcome = proposer.propose(USER_ID, session, call);

        assertThat(outcome.clarificationText()).isNull();
        ProposedActionResponse action = outcome.proposedAction();
        assertThat(action.type()).isEqualTo(ChatActionType.CREATE_PANTRY_ITEM);
        verify(nameResolver, never()).resolve(any(), any());
    }

    @Test
    void propose_updateTool_resolvesByNameAndPreservesOmittedFields() {
        PantryItem existing = PantryItem.builder()
                .id(ITEM_ID).name("Milk")
                .quantity(BigDecimal.valueOf(2)).unit("L")
                .category("dairy").expiryDate(LocalDate.of(2099, 1, 1))
                .build();
        when(nameResolver.resolve(USER_ID, "Milk"))
                .thenReturn(PantryItemNameResolver.Result.found(existing));
        stubPersist(ChatActionType.UPDATE_PANTRY_ITEM);

        AiFunctionCall call = new AiFunctionCall(GeminiProvider.TOOL_UPDATE_PANTRY_ITEM, Map.of(
                "name", "Milk", "quantity", 3));

        ChatActionProposer.Outcome outcome = proposer.propose(USER_ID, session, call);

        UpdatePantryItemActionPayload payload = (UpdatePantryItemActionPayload) outcome.proposedAction().payload();
        assertThat(payload.itemId()).isEqualTo(ITEM_ID);
        assertThat(payload.name()).isEqualTo("Milk");
        assertThat(payload.quantity()).isEqualByComparingTo(BigDecimal.valueOf(3));
        assertThat(payload.unit()).isEqualTo("L");
        assertThat(payload.category()).isEqualTo("dairy");
        assertThat(payload.expiryDate()).isEqualTo(LocalDate.of(2099, 1, 1));
    }

    @Test
    void propose_deleteTool_returnsPayloadWithResolvedItemId() {
        PantryItem existing = PantryItem.builder().id(ITEM_ID).name("Cola")
                .quantity(BigDecimal.ONE).unit("l").build();
        when(nameResolver.resolve(USER_ID, "Cola"))
                .thenReturn(PantryItemNameResolver.Result.found(existing));
        stubPersist(ChatActionType.DELETE_PANTRY_ITEM);

        AiFunctionCall call = new AiFunctionCall(GeminiProvider.TOOL_DELETE_PANTRY_ITEM, Map.of("name", "Cola"));

        ChatActionProposer.Outcome outcome = proposer.propose(USER_ID, session, call);

        DeletePantryItemActionPayload p = (DeletePantryItemActionPayload) outcome.proposedAction().payload();
        assertThat(p.itemId()).isEqualTo(ITEM_ID);
        assertThat(p.name()).isEqualTo("Cola");
    }

    @Test
    void propose_consumeTool_capturesRequestedAmountAndAvailableQuantity() {
        PantryItem existing = PantryItem.builder().id(ITEM_ID).name("Water")
                .quantity(BigDecimal.valueOf(2)).unit("l").build();
        when(nameResolver.resolve(USER_ID, "Water"))
                .thenReturn(PantryItemNameResolver.Result.found(existing));
        stubPersist(ChatActionType.CONSUME_PANTRY_ITEM);

        AiFunctionCall call = new AiFunctionCall(GeminiProvider.TOOL_CONSUME_PANTRY_ITEM, Map.of(
                "name", "Water", "quantity", 1));

        ChatActionProposer.Outcome outcome = proposer.propose(USER_ID, session, call);

        ConsumePantryItemActionPayload p = (ConsumePantryItemActionPayload) outcome.proposedAction().payload();
        assertThat(p.itemId()).isEqualTo(ITEM_ID);
        assertThat(p.quantity()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(p.availableQuantity()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(p.unit()).isEqualTo("l");
    }

    @Test
    void propose_updateTool_whenItemNotFound_returnsClarificationInsteadOfProposal() {
        when(nameResolver.resolve(USER_ID, "Zebra"))
                .thenReturn(PantryItemNameResolver.Result.notFound());
        AiFunctionCall call = new AiFunctionCall(GeminiProvider.TOOL_UPDATE_PANTRY_ITEM, Map.of(
                "name", "Zebra", "quantity", 1));

        ChatActionProposer.Outcome outcome = proposer.propose(USER_ID, session, call);

        assertThat(outcome.proposedAction()).isNull();
        assertThat(outcome.clarificationText()).contains("Zebra");
        verify(actionService, never()).propose(any(), any(), any());
    }

    @Test
    void propose_deleteTool_whenAmbiguous_returnsClarificationListingCandidates() {
        PantryItem a = PantryItem.builder().id(1L).name("Milk").quantity(BigDecimal.valueOf(2)).unit("L").build();
        PantryItem b = PantryItem.builder().id(2L).name("Milk").quantity(BigDecimal.valueOf(1)).unit("L").build();
        when(nameResolver.resolve(USER_ID, "Milk"))
                .thenReturn(PantryItemNameResolver.Result.ambiguous(List.of(a, b)));
        AiFunctionCall call = new AiFunctionCall(GeminiProvider.TOOL_DELETE_PANTRY_ITEM, Map.of("name", "Milk"));

        ChatActionProposer.Outcome outcome = proposer.propose(USER_ID, session, call);

        assertThat(outcome.proposedAction()).isNull();
        assertThat(outcome.clarificationText()).contains("Milk").contains("2").contains("1");
        verify(actionService, never()).propose(any(), any(), any());
    }

    @Test
    void propose_consumeTool_withZeroOrMissingQuantity_returnsNoneWithoutPersisting() {
        PantryItem existing = PantryItem.builder().id(ITEM_ID).name("Water")
                .quantity(BigDecimal.valueOf(2)).unit("l").build();
        when(nameResolver.resolve(USER_ID, "Water"))
                .thenReturn(PantryItemNameResolver.Result.found(existing));

        AiFunctionCall call = new AiFunctionCall(GeminiProvider.TOOL_CONSUME_PANTRY_ITEM, Map.of(
                "name", "Water", "quantity", 0));

        ChatActionProposer.Outcome outcome = proposer.propose(USER_ID, session, call);

        assertThat(outcome.proposedAction()).isNull();
        assertThat(outcome.clarificationText()).isNull();
        verify(actionService, never()).propose(any(), any(), any());
    }

    @Test
    void propose_unknownTool_logsAndReturnsNone() {
        AiFunctionCall call = new AiFunctionCall("mystery_tool", Map.of());

        ChatActionProposer.Outcome outcome = proposer.propose(USER_ID, session, call);

        assertThat(outcome.proposedAction()).isNull();
        assertThat(outcome.clarificationText()).isNull();
        verify(actionService, never()).propose(any(), any(), any());
        verify(nameResolver, never()).resolve(any(), any());
    }

    private void stubPersist(ChatActionType type) {
        when(actionService.propose(eq(session), eq(type), any())).thenAnswer(inv -> ChatAction.builder()
                .id(ACTION_ID)
                .session(session)
                .type(type)
                .status(ChatActionStatus.PENDING)
                .payloadJson(inv.getArgument(2))
                .build());
    }
}
