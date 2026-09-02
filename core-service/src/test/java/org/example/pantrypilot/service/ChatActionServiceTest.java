package org.example.pantrypilot.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import tools.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.example.pantrypilot.dto.BulkActionPayload;
import org.example.pantrypilot.dto.ConfirmActionResponse;
import org.example.pantrypilot.dto.ConsumePantryItemActionPayload;
import org.example.pantrypilot.dto.ConsumeQuantityRequest;
import org.example.pantrypilot.dto.CreatePantryItemRequest;
import org.example.pantrypilot.dto.DeletePantryItemActionPayload;
import org.example.pantrypilot.dto.PantryItemResponse;
import org.example.pantrypilot.dto.UpdatePantryItemActionPayload;
import org.example.pantrypilot.dto.UpdatePantryItemRequest;
import org.example.pantrypilot.model.ChatAction;
import org.example.pantrypilot.model.ChatActionStatus;
import org.example.pantrypilot.model.ChatActionType;
import org.example.pantrypilot.model.ChatSession;
import org.example.pantrypilot.model.User;
import org.example.pantrypilot.repository.ChatActionRepository;
import org.example.pantrypilot.service.exception.InsufficientQuantityException;
import org.example.pantrypilot.service.exception.NotFoundException;
import org.example.pantrypilot.service.exception.StaleChatActionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatActionServiceTest {

    private static final Long USER_ID = 42L;
    private static final Long ACTION_ID = 501L;
    private static final Long OTHER_USER_ID = 99L;

    @Mock private ChatActionRepository actionRepository;
    @Mock private PantryItemService pantryItemService;
    @Mock private ShoppingListService shoppingListService;
    @Mock private ShoppingListItemService shoppingListItemService;
    @Mock private RecipeService recipeService;
    @Mock private RecipeIngredientService recipeIngredientService;

    private ObjectMapper objectMapper;
    private Validator validator;
    private ChatActionService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        service = new ChatActionService(actionRepository, pantryItemService,
                shoppingListService, shoppingListItemService, recipeService, recipeIngredientService,
                objectMapper, validator);
    }

    @Test
    void confirm_pendingActionOwnedByUser_executesAndMarksConfirmed() throws Exception {
        CreatePantryItemRequest req = new CreatePantryItemRequest(
                "Milk", BigDecimal.valueOf(2), "L", "dairy", LocalDate.of(2099, 1, 1));
        ChatAction action = ChatAction.builder()
                .id(ACTION_ID)
                .session(ChatSession.builder().user(User.builder().id(USER_ID).build()).build())
                .type(ChatActionType.CREATE_PANTRY_ITEM)
                .status(ChatActionStatus.PENDING)
                .payloadJson(objectMapper.writeValueAsString(req))
                .build();
        when(actionRepository.findByIdAndUserId(ACTION_ID, USER_ID)).thenReturn(Optional.of(action));

        PantryItemResponse expected = new PantryItemResponse(
                7L, "Milk", BigDecimal.valueOf(2), "L", "dairy",
                LocalDate.of(2099, 1, 1), OffsetDateTime.now(), OffsetDateTime.now());
        when(pantryItemService.createItem(eq(USER_ID), any(CreatePantryItemRequest.class))).thenReturn(expected);

        ConfirmActionResponse actual = service.confirm(USER_ID, ACTION_ID);

        assertThat(actual.result()).isSameAs(expected);
        ArgumentCaptor<CreatePantryItemRequest> reqCap = ArgumentCaptor.forClass(CreatePantryItemRequest.class);
        verify(pantryItemService).createItem(eq(USER_ID), reqCap.capture());
        assertThat(reqCap.getValue().name()).isEqualTo("Milk");
        assertThat(reqCap.getValue().unit()).isEqualTo("L");

        assertThat(action.getStatus()).isEqualTo(ChatActionStatus.CONFIRMED);
        assertThat(action.getConfirmedAt()).isNotNull();
        verify(actionRepository).save(action);
    }

    @Test
    void confirm_actionOwnedByAnotherUser_throwsNotFound() {
        when(actionRepository.findByIdAndUserId(ACTION_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm(OTHER_USER_ID, ACTION_ID))
                .isInstanceOf(NotFoundException.class);
        verify(pantryItemService, never()).createItem(any(), any());
    }

    @Test
    void confirm_alreadyConfirmedAction_throwsStaleAndDoesNotReExecute() throws Exception {
        CreatePantryItemRequest req = new CreatePantryItemRequest(
                "Milk", BigDecimal.ONE, "L", null, null);
        ChatAction action = ChatAction.builder()
                .id(ACTION_ID)
                .session(ChatSession.builder().user(User.builder().id(USER_ID).build()).build())
                .type(ChatActionType.CREATE_PANTRY_ITEM)
                .status(ChatActionStatus.CONFIRMED)
                .payloadJson(objectMapper.writeValueAsString(req))
                .build();
        when(actionRepository.findByIdAndUserId(ACTION_ID, USER_ID)).thenReturn(Optional.of(action));

        assertThatThrownBy(() -> service.confirm(USER_ID, ACTION_ID))
                .isInstanceOf(StaleChatActionException.class);
        verify(pantryItemService, never()).createItem(any(), any());
    }

    @Test
    void confirm_invalidPayload_rejectsBeforeCallingService() throws Exception {
        String bogus = objectMapper.writeValueAsString(new CreatePantryItemRequest(
                " ", BigDecimal.valueOf(-1), "", null, null));
        ChatAction action = ChatAction.builder()
                .id(ACTION_ID)
                .session(ChatSession.builder().user(User.builder().id(USER_ID).build()).build())
                .type(ChatActionType.CREATE_PANTRY_ITEM)
                .status(ChatActionStatus.PENDING)
                .payloadJson(bogus)
                .build();
        when(actionRepository.findByIdAndUserId(ACTION_ID, USER_ID)).thenReturn(Optional.of(action));

        assertThatThrownBy(() -> service.confirm(USER_ID, ACTION_ID))
                .isInstanceOf(StaleChatActionException.class);
        verify(pantryItemService, never()).createItem(any(), any());
    }

    @Test
    void propose_persistsPendingAction() {
        ChatSession session = ChatSession.builder().user(User.builder().id(USER_ID).build()).build();
        when(actionRepository.save(any(ChatAction.class))).thenAnswer(inv -> {
            ChatAction in = inv.getArgument(0);
            in.setId(ACTION_ID);
            return in;
        });

        ChatAction saved = service.propose(session, ChatActionType.CREATE_PANTRY_ITEM, "{\"name\":\"Milk\"}");

        assertThat(saved.getId()).isEqualTo(ACTION_ID);
        assertThat(saved.getStatus()).isEqualTo(ChatActionStatus.PENDING);
        assertThat(saved.getSession()).isSameAs(session);
    }

    @Test
    void confirm_updateAction_dispatchesToUpdateWithResolvedItemId() throws Exception {
        Long itemId = 88L;
        UpdatePantryItemActionPayload payload = new UpdatePantryItemActionPayload(
                itemId, "Milk", BigDecimal.valueOf(3), "L", "dairy", LocalDate.of(2099, 1, 1));
        ChatAction action = ChatAction.builder()
                .id(ACTION_ID)
                .session(ChatSession.builder().user(User.builder().id(USER_ID).build()).build())
                .type(ChatActionType.UPDATE_PANTRY_ITEM)
                .status(ChatActionStatus.PENDING)
                .payloadJson(objectMapper.writeValueAsString(payload))
                .build();
        when(actionRepository.findByIdAndUserId(ACTION_ID, USER_ID)).thenReturn(Optional.of(action));
        PantryItemResponse expected = new PantryItemResponse(
                itemId, "Milk", BigDecimal.valueOf(3), "L", "dairy",
                LocalDate.of(2099, 1, 1), OffsetDateTime.now(), OffsetDateTime.now());
        when(pantryItemService.updateItem(eq(USER_ID), eq(itemId), any(UpdatePantryItemRequest.class)))
                .thenReturn(expected);

        ConfirmActionResponse actual = service.confirm(USER_ID, ACTION_ID);

        assertThat(actual.result()).isSameAs(expected);
        ArgumentCaptor<UpdatePantryItemRequest> cap =
                ArgumentCaptor.forClass(UpdatePantryItemRequest.class);
        verify(pantryItemService).updateItem(eq(USER_ID), eq(itemId), cap.capture());
        assertThat(cap.getValue().name()).isEqualTo("Milk");
        assertThat(cap.getValue().quantity()).isEqualByComparingTo(BigDecimal.valueOf(3));
        assertThat(action.getStatus()).isEqualTo(ChatActionStatus.CONFIRMED);
    }

    @Test
    void confirm_deleteAction_dispatchesToDeleteAndReturnsNull() throws Exception {
        Long itemId = 12L;
        DeletePantryItemActionPayload payload = new DeletePantryItemActionPayload(itemId, "Cola");
        ChatAction action = ChatAction.builder()
                .id(ACTION_ID)
                .session(ChatSession.builder().user(User.builder().id(USER_ID).build()).build())
                .type(ChatActionType.DELETE_PANTRY_ITEM)
                .status(ChatActionStatus.PENDING)
                .payloadJson(objectMapper.writeValueAsString(payload))
                .build();
        when(actionRepository.findByIdAndUserId(ACTION_ID, USER_ID)).thenReturn(Optional.of(action));

        ConfirmActionResponse actual = service.confirm(USER_ID, ACTION_ID);

        assertThat(actual.actionType()).isEqualTo(ChatActionType.DELETE_PANTRY_ITEM);
        assertThat(actual.result()).isNull();
        verify(pantryItemService).deleteItem(USER_ID, itemId);
        assertThat(action.getStatus()).isEqualTo(ChatActionStatus.CONFIRMED);
    }

    @Test
    void confirm_consumeAction_dispatchesToConsumeWithRequestedQuantity() throws Exception {
        Long itemId = 33L;
        ConsumePantryItemActionPayload payload = new ConsumePantryItemActionPayload(
                itemId, "Water", BigDecimal.valueOf(1), "L", BigDecimal.valueOf(2));
        ChatAction action = ChatAction.builder()
                .id(ACTION_ID)
                .session(ChatSession.builder().user(User.builder().id(USER_ID).build()).build())
                .type(ChatActionType.CONSUME_PANTRY_ITEM)
                .status(ChatActionStatus.PENDING)
                .payloadJson(objectMapper.writeValueAsString(payload))
                .build();
        when(actionRepository.findByIdAndUserId(ACTION_ID, USER_ID)).thenReturn(Optional.of(action));
        PantryItemResponse expected = new PantryItemResponse(
                itemId, "Water", BigDecimal.valueOf(1), "L", "other",
                null, OffsetDateTime.now(), OffsetDateTime.now());
        when(pantryItemService.consumeQuantity(eq(USER_ID), eq(itemId), any(ConsumeQuantityRequest.class)))
                .thenReturn(expected);

        ConfirmActionResponse actual = service.confirm(USER_ID, ACTION_ID);

        assertThat(actual.result()).isSameAs(expected);
        ArgumentCaptor<ConsumeQuantityRequest> cap =
                ArgumentCaptor.forClass(ConsumeQuantityRequest.class);
        verify(pantryItemService).consumeQuantity(eq(USER_ID), eq(itemId), cap.capture());
        assertThat(cap.getValue().quantity()).isEqualByComparingTo(BigDecimal.valueOf(1));
        assertThat(action.getStatus()).isEqualTo(ChatActionStatus.CONFIRMED);
    }

    @Test
    void confirm_consumeAction_whenExceedsAvailable_propagatesInsufficientQuantity() throws Exception {
        Long itemId = 33L;
        ConsumePantryItemActionPayload payload = new ConsumePantryItemActionPayload(
                itemId, "Water", BigDecimal.valueOf(10), "L", BigDecimal.valueOf(2));
        ChatAction action = ChatAction.builder()
                .id(ACTION_ID)
                .session(ChatSession.builder().user(User.builder().id(USER_ID).build()).build())
                .type(ChatActionType.CONSUME_PANTRY_ITEM)
                .status(ChatActionStatus.PENDING)
                .payloadJson(objectMapper.writeValueAsString(payload))
                .build();
        when(actionRepository.findByIdAndUserId(ACTION_ID, USER_ID)).thenReturn(Optional.of(action));
        when(pantryItemService.consumeQuantity(eq(USER_ID), eq(itemId), any(ConsumeQuantityRequest.class)))
                .thenThrow(new InsufficientQuantityException("Consume quantity exceeds available quantity"));

        assertThatThrownBy(() -> service.confirm(USER_ID, ACTION_ID))
                .isInstanceOf(InsufficientQuantityException.class);
        assertThat(action.getStatus()).isEqualTo(ChatActionStatus.PENDING);
    }

    @Test
    void confirm_bulkDelete_allTargetsSucceed_marksConfirmedAndReportsCount() throws Exception {
        BulkActionPayload payload = new BulkActionPayload(
                ChatActionType.DELETE_PANTRY_ITEM,
                java.util.List.of(
                        new DeletePantryItemActionPayload(101L, "Milk"),
                        new DeletePantryItemActionPayload(102L, "Bread")),
                "Empty pantry (2 items)");
        ChatAction action = ChatAction.builder()
                .id(ACTION_ID)
                .session(ChatSession.builder().user(User.builder().id(USER_ID).build()).build())
                .type(ChatActionType.BULK_ACTION)
                .status(ChatActionStatus.PENDING)
                .payloadJson(objectMapper.writeValueAsString(payload))
                .build();
        when(actionRepository.findByIdAndUserId(ACTION_ID, USER_ID)).thenReturn(Optional.of(action));

        ConfirmActionResponse actual = service.confirm(USER_ID, ACTION_ID);

        assertThat(actual.actionType()).isEqualTo(ChatActionType.BULK_ACTION);
        assertThat(actual.bulkResult()).isNotNull();
        assertThat(actual.bulkResult().succeeded()).isEqualTo(2);
        assertThat(actual.bulkResult().failed()).isEqualTo(0);
        verify(pantryItemService).deleteItem(USER_ID, 101L);
        verify(pantryItemService).deleteItem(USER_ID, 102L);
        assertThat(action.getStatus()).isEqualTo(ChatActionStatus.CONFIRMED);
    }

    @Test
    void confirm_bulkDelete_partialFailure_continuesAndReportsFailures() throws Exception {
        BulkActionPayload payload = new BulkActionPayload(
                ChatActionType.DELETE_PANTRY_ITEM,
                java.util.List.of(
                        new DeletePantryItemActionPayload(201L, "A"),
                        new DeletePantryItemActionPayload(202L, "B"),
                        new DeletePantryItemActionPayload(203L, "C")),
                "Empty pantry (3 items)");
        ChatAction action = ChatAction.builder()
                .id(ACTION_ID)
                .session(ChatSession.builder().user(User.builder().id(USER_ID).build()).build())
                .type(ChatActionType.BULK_ACTION)
                .status(ChatActionStatus.PENDING)
                .payloadJson(objectMapper.writeValueAsString(payload))
                .build();
        when(actionRepository.findByIdAndUserId(ACTION_ID, USER_ID)).thenReturn(Optional.of(action));
        org.mockito.Mockito.doAnswer(inv -> {
            Long id = inv.getArgument(1);
            if (id == 202L) {
                throw new NotFoundException("Pantry item not found");
            }
            return null;
        }).when(pantryItemService).deleteItem(eq(USER_ID), any(Long.class));

        ConfirmActionResponse actual = service.confirm(USER_ID, ACTION_ID);

        assertThat(actual.bulkResult()).isNotNull();
        assertThat(actual.bulkResult().succeeded()).isEqualTo(2);
        assertThat(actual.bulkResult().failed()).isEqualTo(1);
        assertThat(actual.bulkResult().failures()).hasSize(1);
        assertThat(actual.bulkResult().failures().get(0).reason()).contains("not found");
        verify(pantryItemService).deleteItem(USER_ID, 201L);
        verify(pantryItemService).deleteItem(USER_ID, 202L);
        verify(pantryItemService).deleteItem(USER_ID, 203L);
        assertThat(action.getStatus()).isEqualTo(ChatActionStatus.CONFIRMED);
    }
}
