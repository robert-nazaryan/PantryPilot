package org.example.pantrypilot.service.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.ObjectMapper;
import org.example.pantrypilot.dto.AddShoppingListItemActionPayload;
import org.example.pantrypilot.dto.BulkActionPayload;
import org.example.pantrypilot.dto.ConsumePantryItemActionPayload;
import org.example.pantrypilot.dto.CreateShoppingListActionPayload;
import org.example.pantrypilot.dto.DeletePantryItemActionPayload;
import org.example.pantrypilot.dto.GenerateShoppingListFromRecipeActionPayload;
import org.example.pantrypilot.dto.ProposedActionResponse;
import org.example.pantrypilot.dto.UpdatePantryItemActionPayload;
import org.example.pantrypilot.model.ChatAction;
import org.example.pantrypilot.model.ChatActionStatus;
import org.example.pantrypilot.model.ChatActionType;
import org.example.pantrypilot.model.ChatSession;
import org.example.pantrypilot.model.PantryItem;
import org.example.pantrypilot.model.Recipe;
import org.example.pantrypilot.model.ShoppingList;
import org.example.pantrypilot.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.example.pantrypilot.repository.PantryItemRepository;
import org.example.pantrypilot.repository.RecipeIngredientRepository;
import org.example.pantrypilot.repository.RecipeRepository;
import org.example.pantrypilot.repository.ShoppingListRepository;
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
    @Mock private PantryItemRepository pantryItemRepository;
    @Mock private ShoppingListRepository shoppingListRepository;
    @Mock private RecipeRepository recipeRepository;
    @Mock private RecipeIngredientRepository recipeIngredientRepository;

    private ObjectMapper objectMapper;
    private ChatActionProposer proposer;
    private ChatSession session;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        proposer = new ChatActionProposer(actionService, nameResolver,
                pantryItemRepository, shoppingListRepository, recipeRepository,
                recipeIngredientRepository, objectMapper);
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

    @Test
    void propose_bulkDeletePantryItems_scopeAll_producesBulkActionWithAllItems() {
        PantryItem a = PantryItem.builder().id(1L).name("Milk")
                .quantity(BigDecimal.valueOf(2)).unit("L").build();
        PantryItem b = PantryItem.builder().id(2L).name("Bread")
                .quantity(BigDecimal.ONE).unit("loaf").build();
        Page<PantryItem> page = new PageImpl<>(List.of(a, b));
        when(pantryItemRepository.findByUserIdOrderByExpiryDateAscNullsLast(eq(USER_ID), any()))
                .thenReturn(page);
        stubPersist(ChatActionType.BULK_ACTION);

        AiFunctionCall call = new AiFunctionCall(
                GeminiProvider.TOOL_BULK_DELETE_PANTRY_ITEMS, Map.of("scope", "all"));

        ChatActionProposer.Outcome outcome = proposer.propose(USER_ID, session, call);

        assertThat(outcome.proposedAction()).isNotNull();
        BulkActionPayload bulk = (BulkActionPayload) outcome.proposedAction().payload();
        assertThat(bulk.subActionType()).isEqualTo(ChatActionType.DELETE_PANTRY_ITEM);
        assertThat(bulk.targets()).hasSize(2);
        assertThat(bulk.summary()).contains("2");
    }

    @Test
    void propose_bulkDeletePantryItems_scopeByName_producesBulkForAmbiguousMatches() {
        PantryItem milk1 = PantryItem.builder().id(1L).name("Milk")
                .quantity(BigDecimal.valueOf(2)).unit("L").build();
        PantryItem milk2 = PantryItem.builder().id(2L).name("Milk")
                .quantity(BigDecimal.ONE).unit("L").build();
        when(pantryItemRepository.findByUserIdAndNameIgnoreCase(USER_ID, "Milk"))
                .thenReturn(List.of(milk1, milk2));
        stubPersist(ChatActionType.BULK_ACTION);

        AiFunctionCall call = new AiFunctionCall(GeminiProvider.TOOL_BULK_DELETE_PANTRY_ITEMS,
                Map.of("scope", "byName", "nameFilter", "Milk"));

        ChatActionProposer.Outcome outcome = proposer.propose(USER_ID, session, call);

        BulkActionPayload bulk = (BulkActionPayload) outcome.proposedAction().payload();
        assertThat(bulk.subActionType()).isEqualTo(ChatActionType.DELETE_PANTRY_ITEM);
        assertThat(bulk.targets()).hasSize(2);
    }

    @Test
    void propose_bulkDeletePantryItems_noMatches_returnsClarification() {
        when(pantryItemRepository.findByUserIdAndNameIgnoreCase(USER_ID, "Ghost"))
                .thenReturn(List.of());
        AiFunctionCall call = new AiFunctionCall(GeminiProvider.TOOL_BULK_DELETE_PANTRY_ITEMS,
                Map.of("scope", "byName", "nameFilter", "Ghost"));

        ChatActionProposer.Outcome outcome = proposer.propose(USER_ID, session, call);

        assertThat(outcome.proposedAction()).isNull();
        assertThat(outcome.clarificationText()).contains("Nothing to remove");
    }

    @Test
    void propose_createShoppingList_persistsWithName() {
        stubPersist(ChatActionType.CREATE_SHOPPING_LIST);
        AiFunctionCall call = new AiFunctionCall(
                GeminiProvider.TOOL_CREATE_SHOPPING_LIST, Map.of("name", "Groceries"));

        ChatActionProposer.Outcome outcome = proposer.propose(USER_ID, session, call);

        CreateShoppingListActionPayload p =
                (CreateShoppingListActionPayload) outcome.proposedAction().payload();
        assertThat(p.name()).isEqualTo("Groceries");
    }

    @Test
    void propose_addShoppingListItem_resolvesListByName() {
        ShoppingList list = ShoppingList.builder().id(7L).name("Groceries")
                .user(User.builder().id(USER_ID).build()).build();
        when(shoppingListRepository.findByUserIdAndNameIgnoreCase(USER_ID, "Groceries"))
                .thenReturn(List.of(list));
        stubPersist(ChatActionType.ADD_SHOPPING_LIST_ITEM);

        AiFunctionCall call = new AiFunctionCall(GeminiProvider.TOOL_ADD_SHOPPING_LIST_ITEM, Map.of(
                "listName", "Groceries", "name", "Pepperoni", "quantity", 200, "unit", "g"));

        ChatActionProposer.Outcome outcome = proposer.propose(USER_ID, session, call);

        AddShoppingListItemActionPayload p =
                (AddShoppingListItemActionPayload) outcome.proposedAction().payload();
        assertThat(p.listId()).isEqualTo(7L);
        assertThat(p.listName()).isEqualTo("Groceries");
        assertThat(p.name()).isEqualTo("Pepperoni");
        assertThat(p.quantity()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }

    @Test
    void propose_addShoppingListItem_unknownList_returnsClarification() {
        when(shoppingListRepository.findByUserIdAndNameIgnoreCase(USER_ID, "Nope"))
                .thenReturn(List.of());
        AiFunctionCall call = new AiFunctionCall(GeminiProvider.TOOL_ADD_SHOPPING_LIST_ITEM, Map.of(
                "listName", "Nope", "name", "Sugar"));

        ChatActionProposer.Outcome outcome = proposer.propose(USER_ID, session, call);

        assertThat(outcome.proposedAction()).isNull();
        assertThat(outcome.clarificationText()).contains("Nope");
    }

    @Test
    void propose_generateShoppingListFromRecipe_resolvesRecipeByTitle() {
        Recipe recipe = Recipe.builder().id(9L).title("Pizza")
                .user(User.builder().id(USER_ID).build()).build();
        when(recipeRepository.findByUserIdAndTitleIgnoreCase(USER_ID, "Pizza"))
                .thenReturn(List.of(recipe));
        stubPersist(ChatActionType.GENERATE_SHOPPING_LIST_FROM_RECIPE);

        AiFunctionCall call = new AiFunctionCall(
                GeminiProvider.TOOL_GENERATE_SHOPPING_LIST_FROM_RECIPE,
                Map.of("recipeTitle", "Pizza"));

        ChatActionProposer.Outcome outcome = proposer.propose(USER_ID, session, call);

        GenerateShoppingListFromRecipeActionPayload p =
                (GenerateShoppingListFromRecipeActionPayload) outcome.proposedAction().payload();
        assertThat(p.recipeId()).isEqualTo(9L);
        assertThat(p.recipeTitle()).isEqualTo("Pizza");
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
