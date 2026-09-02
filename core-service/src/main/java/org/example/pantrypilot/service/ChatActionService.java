package org.example.pantrypilot.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.pantrypilot.dto.AddRecipeIngredientActionPayload;
import org.example.pantrypilot.dto.AddShoppingListItemActionPayload;
import org.example.pantrypilot.dto.BulkActionPayload;
import org.example.pantrypilot.dto.BulkActionResult;
import org.example.pantrypilot.dto.ConfirmActionResponse;
import org.example.pantrypilot.dto.ConsumePantryItemActionPayload;
import org.example.pantrypilot.dto.ConsumeQuantityRequest;
import org.example.pantrypilot.dto.CreatePantryItemRequest;
import org.example.pantrypilot.dto.CreateRecipeActionPayload;
import org.example.pantrypilot.dto.CreateRecipeIngredientRequest;
import org.example.pantrypilot.dto.CreateRecipeRequest;
import org.example.pantrypilot.dto.CreateShoppingListActionPayload;
import org.example.pantrypilot.dto.CreateShoppingListItemRequest;
import org.example.pantrypilot.dto.CreateShoppingListRequest;
import org.example.pantrypilot.dto.DeletePantryItemActionPayload;
import org.example.pantrypilot.dto.DeleteRecipeActionPayload;
import org.example.pantrypilot.dto.GenerateShoppingListFromRecipeActionPayload;
import org.example.pantrypilot.dto.RemoveRecipeIngredientActionPayload;
import org.example.pantrypilot.dto.RemoveShoppingListItemActionPayload;
import org.example.pantrypilot.dto.SetShoppingListItemCheckedActionPayload;
import org.example.pantrypilot.dto.ToggleShoppingListItemCheckedRequest;
import org.example.pantrypilot.dto.UpdatePantryItemActionPayload;
import org.example.pantrypilot.dto.UpdatePantryItemRequest;
import org.example.pantrypilot.model.ChatAction;
import org.example.pantrypilot.model.ChatActionStatus;
import org.example.pantrypilot.model.ChatActionType;
import org.example.pantrypilot.model.ChatSession;
import org.example.pantrypilot.repository.ChatActionRepository;
import org.example.pantrypilot.service.exception.NotFoundException;
import org.example.pantrypilot.service.exception.StaleChatActionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatActionService {

    private final ChatActionRepository actionRepository;
    private final PantryItemService pantryItemService;
    private final ShoppingListService shoppingListService;
    private final ShoppingListItemService shoppingListItemService;
    private final RecipeService recipeService;
    private final RecipeIngredientService recipeIngredientService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Transactional
    public ChatAction propose(ChatSession session, ChatActionType type, String payloadJson) {
        return actionRepository.save(ChatAction.builder()
                .session(session)
                .type(type)
                .status(ChatActionStatus.PENDING)
                .payloadJson(payloadJson)
                .build());
    }

    @Transactional
    public ConfirmActionResponse confirm(Long userId, Long actionId) {
        ChatAction action = actionRepository.findByIdAndUserId(actionId, userId)
                .orElseThrow(() -> new NotFoundException("Chat action not found"));
        if (action.getStatus() != ChatActionStatus.PENDING) {
            throw new StaleChatActionException("Chat action has already been confirmed");
        }
        ConfirmActionResponse response = executeByType(userId, action);
        action.setStatus(ChatActionStatus.CONFIRMED);
        action.setConfirmedAt(OffsetDateTime.now());
        actionRepository.save(action);
        return response;
    }

    private ConfirmActionResponse executeByType(Long userId, ChatAction action) {
        return switch (action.getType()) {
            case CREATE_PANTRY_ITEM -> ConfirmActionResponse.single(action.getType(),
                    executeCreatePantryItem(userId, action.getPayloadJson()));
            case UPDATE_PANTRY_ITEM -> ConfirmActionResponse.single(action.getType(),
                    executeUpdatePantryItem(userId, action.getPayloadJson()));
            case DELETE_PANTRY_ITEM -> {
                executeDeletePantryItem(userId, action.getPayloadJson());
                yield ConfirmActionResponse.single(action.getType(), null);
            }
            case CONSUME_PANTRY_ITEM -> ConfirmActionResponse.single(action.getType(),
                    executeConsumePantryItem(userId, action.getPayloadJson()));
            case CREATE_SHOPPING_LIST -> ConfirmActionResponse.single(action.getType(),
                    executeCreateShoppingList(userId, action.getPayloadJson()));
            case ADD_SHOPPING_LIST_ITEM -> ConfirmActionResponse.single(action.getType(),
                    executeAddShoppingListItem(userId, action.getPayloadJson()));
            case REMOVE_SHOPPING_LIST_ITEM -> {
                executeRemoveShoppingListItem(userId, action.getPayloadJson());
                yield ConfirmActionResponse.single(action.getType(), null);
            }
            case CHECK_SHOPPING_LIST_ITEM, UNCHECK_SHOPPING_LIST_ITEM ->
                    ConfirmActionResponse.single(action.getType(),
                            executeSetShoppingListItemChecked(userId, action.getPayloadJson()));
            case GENERATE_SHOPPING_LIST_FROM_RECIPE -> ConfirmActionResponse.single(action.getType(),
                    executeGenerateShoppingListFromRecipe(userId, action.getPayloadJson()));
            case CREATE_RECIPE -> ConfirmActionResponse.single(action.getType(),
                    executeCreateRecipe(userId, action.getPayloadJson()));
            case DELETE_RECIPE -> {
                executeDeleteRecipe(userId, action.getPayloadJson());
                yield ConfirmActionResponse.single(action.getType(), null);
            }
            case ADD_RECIPE_INGREDIENT -> ConfirmActionResponse.single(action.getType(),
                    executeAddRecipeIngredient(userId, action.getPayloadJson()));
            case REMOVE_RECIPE_INGREDIENT -> {
                executeRemoveRecipeIngredient(userId, action.getPayloadJson());
                yield ConfirmActionResponse.single(action.getType(), null);
            }
            case BULK_ACTION -> ConfirmActionResponse.bulk(executeBulk(userId, action.getPayloadJson()));
        };
    }

    // ---------- Pantry ----------

    private Object executeCreatePantryItem(Long userId, String json) {
        CreatePantryItemRequest req = deserialize(json, CreatePantryItemRequest.class);
        validate(req);
        return pantryItemService.createItem(userId, req);
    }

    private Object executeUpdatePantryItem(Long userId, String json) {
        UpdatePantryItemActionPayload p = deserialize(json, UpdatePantryItemActionPayload.class);
        UpdatePantryItemRequest req = new UpdatePantryItemRequest(
                p.name(), p.quantity(), p.unit(), p.category(), p.expiryDate());
        validate(req);
        return pantryItemService.updateItem(userId, p.itemId(), req);
    }

    private void executeDeletePantryItem(Long userId, String json) {
        DeletePantryItemActionPayload p = deserialize(json, DeletePantryItemActionPayload.class);
        pantryItemService.deleteItem(userId, p.itemId());
    }

    private Object executeConsumePantryItem(Long userId, String json) {
        ConsumePantryItemActionPayload p = deserialize(json, ConsumePantryItemActionPayload.class);
        ConsumeQuantityRequest req = new ConsumeQuantityRequest(p.quantity());
        validate(req);
        return pantryItemService.consumeQuantity(userId, p.itemId(), req);
    }

    // ---------- Shopping lists ----------

    private Object executeCreateShoppingList(Long userId, String json) {
        CreateShoppingListActionPayload p = deserialize(json, CreateShoppingListActionPayload.class);
        return shoppingListService.createList(userId, new CreateShoppingListRequest(p.name()));
    }

    private Object executeAddShoppingListItem(Long userId, String json) {
        AddShoppingListItemActionPayload p = deserialize(json, AddShoppingListItemActionPayload.class);
        CreateShoppingListItemRequest req = new CreateShoppingListItemRequest(
                p.name(), p.quantity(), p.unit());
        validate(req);
        return shoppingListItemService.addItem(userId, p.listId(), req);
    }

    private void executeRemoveShoppingListItem(Long userId, String json) {
        RemoveShoppingListItemActionPayload p = deserialize(json, RemoveShoppingListItemActionPayload.class);
        shoppingListItemService.deleteItem(userId, p.listId(), p.itemId());
    }

    private Object executeSetShoppingListItemChecked(Long userId, String json) {
        SetShoppingListItemCheckedActionPayload p = deserialize(json, SetShoppingListItemCheckedActionPayload.class);
        return shoppingListItemService.setChecked(userId, p.listId(), p.itemId(),
                new ToggleShoppingListItemCheckedRequest(p.checked()));
    }

    private Object executeGenerateShoppingListFromRecipe(Long userId, String json) {
        GenerateShoppingListFromRecipeActionPayload p = deserialize(
                json, GenerateShoppingListFromRecipeActionPayload.class);
        return shoppingListService.generateFromRecipe(userId, p.recipeId());
    }

    // ---------- Recipes ----------

    private Object executeCreateRecipe(Long userId, String json) {
        CreateRecipeActionPayload p = deserialize(json, CreateRecipeActionPayload.class);
        String[] tags = p.tags() == null ? new String[0] : p.tags().toArray(new String[0]);
        CreateRecipeRequest req = new CreateRecipeRequest(
                p.title(), p.instructions(), p.cookTimeMinutes(), tags);
        validate(req);
        return recipeService.createRecipe(userId, req);
    }

    private void executeDeleteRecipe(Long userId, String json) {
        DeleteRecipeActionPayload p = deserialize(json, DeleteRecipeActionPayload.class);
        recipeService.deleteRecipe(userId, p.recipeId());
    }

    private Object executeAddRecipeIngredient(Long userId, String json) {
        AddRecipeIngredientActionPayload p = deserialize(json, AddRecipeIngredientActionPayload.class);
        CreateRecipeIngredientRequest req = new CreateRecipeIngredientRequest(
                p.name(), p.quantity(), p.unit());
        validate(req);
        return recipeIngredientService.addIngredient(userId, p.recipeId(), req);
    }

    private void executeRemoveRecipeIngredient(Long userId, String json) {
        RemoveRecipeIngredientActionPayload p = deserialize(json, RemoveRecipeIngredientActionPayload.class);
        recipeIngredientService.deleteIngredient(userId, p.recipeId(), p.ingredientId());
    }

    // ---------- Bulk ----------

    private BulkActionResult executeBulk(Long userId, String json) {
        BulkActionPayload bulk = deserialize(json, BulkActionPayload.class);
        JavaType targetType = targetPayloadTypeFor(bulk.subActionType());
        int succeeded = 0;
        List<BulkActionResult.BulkActionFailure> failures = new ArrayList<>();
        for (Object rawTarget : bulk.targets()) {
            try {
                Object typedTarget = objectMapper.convertValue(rawTarget, targetType);
                executeSingleTarget(userId, bulk.subActionType(), typedTarget);
                succeeded++;
            } catch (RuntimeException ex) {
                failures.add(new BulkActionResult.BulkActionFailure(rawTarget, ex.getMessage()));
                log.warn("Bulk target failed ({}): {}", bulk.subActionType(), ex.getMessage());
            }
        }
        return new BulkActionResult(succeeded, failures.size(), failures);
    }

    private JavaType targetPayloadTypeFor(ChatActionType subType) {
        Class<?> payloadClass = switch (subType) {
            case DELETE_PANTRY_ITEM -> DeletePantryItemActionPayload.class;
            case REMOVE_SHOPPING_LIST_ITEM -> RemoveShoppingListItemActionPayload.class;
            case CHECK_SHOPPING_LIST_ITEM, UNCHECK_SHOPPING_LIST_ITEM ->
                    SetShoppingListItemCheckedActionPayload.class;
            default -> throw new StaleChatActionException(
                    "Bulk action does not support subActionType " + subType);
        };
        return objectMapper.getTypeFactory().constructType(payloadClass);
    }

    private void executeSingleTarget(Long userId, ChatActionType subType, Object typedTarget) {
        switch (subType) {
            case DELETE_PANTRY_ITEM -> {
                DeletePantryItemActionPayload p = (DeletePantryItemActionPayload) typedTarget;
                pantryItemService.deleteItem(userId, p.itemId());
            }
            case REMOVE_SHOPPING_LIST_ITEM -> {
                RemoveShoppingListItemActionPayload p = (RemoveShoppingListItemActionPayload) typedTarget;
                shoppingListItemService.deleteItem(userId, p.listId(), p.itemId());
            }
            case CHECK_SHOPPING_LIST_ITEM, UNCHECK_SHOPPING_LIST_ITEM -> {
                SetShoppingListItemCheckedActionPayload p =
                        (SetShoppingListItemCheckedActionPayload) typedTarget;
                shoppingListItemService.setChecked(userId, p.listId(), p.itemId(),
                        new ToggleShoppingListItemCheckedRequest(p.checked()));
            }
            default -> throw new StaleChatActionException(
                    "Bulk action does not support subActionType " + subType);
        }
    }

    // ---------- Common ----------

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JacksonException ex) {
            throw new StaleChatActionException(
                    "Chat action payload is no longer usable: " + ex.getOriginalMessage());
        }
    }

    private <T> void validate(T payload) {
        Set<ConstraintViolation<T>> violations = validator.validate(payload);
        if (!violations.isEmpty()) {
            String detail = violations.iterator().next().getMessage();
            throw new StaleChatActionException("Proposed action failed validation: " + detail);
        }
    }
}
