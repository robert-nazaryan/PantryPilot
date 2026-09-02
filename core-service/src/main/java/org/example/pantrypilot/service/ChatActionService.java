package org.example.pantrypilot.service;

import java.time.OffsetDateTime;
import java.util.Set;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
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
import org.example.pantrypilot.repository.ChatActionRepository;
import org.example.pantrypilot.service.exception.NotFoundException;
import org.example.pantrypilot.service.exception.StaleChatActionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatActionService {

    private final ChatActionRepository actionRepository;
    private final PantryItemService pantryItemService;
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
        PantryItemResponse item = executeByType(userId, action);
        action.setStatus(ChatActionStatus.CONFIRMED);
        action.setConfirmedAt(OffsetDateTime.now());
        actionRepository.save(action);
        return new ConfirmActionResponse(action.getType(), item);
    }

    private PantryItemResponse executeByType(Long userId, ChatAction action) {
        return switch (action.getType()) {
            case CREATE_PANTRY_ITEM -> executeCreate(userId, action);
            case UPDATE_PANTRY_ITEM -> executeUpdate(userId, action);
            case DELETE_PANTRY_ITEM -> executeDelete(userId, action);
            case CONSUME_PANTRY_ITEM -> executeConsume(userId, action);
        };
    }

    private PantryItemResponse executeCreate(Long userId, ChatAction action) {
        CreatePantryItemRequest req = deserializePayload(action, CreatePantryItemRequest.class);
        validate(req);
        return pantryItemService.createItem(userId, req);
    }

    private PantryItemResponse executeUpdate(Long userId, ChatAction action) {
        UpdatePantryItemActionPayload p = deserializePayload(action, UpdatePantryItemActionPayload.class);
        UpdatePantryItemRequest req = new UpdatePantryItemRequest(
                p.name(), p.quantity(), p.unit(), p.category(), p.expiryDate());
        validate(req);
        return pantryItemService.updateItem(userId, p.itemId(), req);
    }

    private PantryItemResponse executeDelete(Long userId, ChatAction action) {
        DeletePantryItemActionPayload p = deserializePayload(action, DeletePantryItemActionPayload.class);
        pantryItemService.deleteItem(userId, p.itemId());
        return null;
    }

    private PantryItemResponse executeConsume(Long userId, ChatAction action) {
        ConsumePantryItemActionPayload p = deserializePayload(action, ConsumePantryItemActionPayload.class);
        ConsumeQuantityRequest req = new ConsumeQuantityRequest(p.quantity());
        validate(req);
        return pantryItemService.consumeQuantity(userId, p.itemId(), req);
    }

    private <T> T deserializePayload(ChatAction action, Class<T> type) {
        try {
            return objectMapper.readValue(action.getPayloadJson(), type);
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
