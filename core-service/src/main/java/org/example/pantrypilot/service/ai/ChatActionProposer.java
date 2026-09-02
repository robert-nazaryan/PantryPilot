package org.example.pantrypilot.service.ai;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.pantrypilot.dto.ConsumePantryItemActionPayload;
import org.example.pantrypilot.dto.CreatePantryItemRequest;
import org.example.pantrypilot.dto.DeletePantryItemActionPayload;
import org.example.pantrypilot.dto.ProposedActionResponse;
import org.example.pantrypilot.dto.UpdatePantryItemActionPayload;
import org.example.pantrypilot.model.ChatAction;
import org.example.pantrypilot.model.ChatActionType;
import org.example.pantrypilot.model.ChatSession;
import org.example.pantrypilot.model.PantryItem;
import org.example.pantrypilot.service.ChatActionService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatActionProposer {

    private final ChatActionService actionService;
    private final PantryItemNameResolver nameResolver;
    private final ObjectMapper objectMapper;

    public record Outcome(ProposedActionResponse proposedAction, String clarificationText) {
        public static Outcome none() {
            return new Outcome(null, null);
        }

        public static Outcome proposed(ProposedActionResponse action) {
            return new Outcome(action, null);
        }

        public static Outcome clarify(String text) {
            return new Outcome(null, text);
        }
    }

    public Outcome propose(Long userId, ChatSession session, AiFunctionCall call) {
        return switch (call.name()) {
            case GeminiProvider.TOOL_CREATE_PANTRY_ITEM -> proposeCreate(session, call);
            case GeminiProvider.TOOL_UPDATE_PANTRY_ITEM -> proposeUpdate(userId, session, call);
            case GeminiProvider.TOOL_DELETE_PANTRY_ITEM -> proposeDelete(userId, session, call);
            case GeminiProvider.TOOL_CONSUME_PANTRY_ITEM -> proposeConsume(userId, session, call);
            default -> {
                log.warn("Ignoring unknown function call from model: {}", call.name());
                yield Outcome.none();
            }
        };
    }

    private Outcome proposeCreate(ChatSession session, AiFunctionCall call) {
        CreatePantryItemRequest payload = convert(call, CreatePantryItemRequest.class);
        if (payload == null) {
            return Outcome.none();
        }
        return persist(session, ChatActionType.CREATE_PANTRY_ITEM, payload);
    }

    private Outcome proposeUpdate(Long userId, ChatSession session, AiFunctionCall call) {
        String name = stringArg(call, "name");
        PantryItemNameResolver.Result resolution = nameResolver.resolve(userId, name);
        Outcome clarification = clarificationOrNull(name, resolution);
        if (clarification != null) {
            return clarification;
        }
        PantryItem existing = resolution.item();
        UpdatePantryItemActionPayload payload = new UpdatePantryItemActionPayload(
                existing.getId(),
                existing.getName(),
                bigDecimalArg(call, "quantity", existing.getQuantity()),
                stringArg(call, "unit", existing.getUnit()),
                nullableStringArg(call, "category", existing.getCategory()),
                nullableLocalDateArg(call, "expiryDate",
                        existing.getExpiryDate() == null ? null : existing.getExpiryDate().toString()));
        return persist(session, ChatActionType.UPDATE_PANTRY_ITEM, payload);
    }

    private Outcome proposeDelete(Long userId, ChatSession session, AiFunctionCall call) {
        String name = stringArg(call, "name");
        PantryItemNameResolver.Result resolution = nameResolver.resolve(userId, name);
        Outcome clarification = clarificationOrNull(name, resolution);
        if (clarification != null) {
            return clarification;
        }
        PantryItem existing = resolution.item();
        return persist(session, ChatActionType.DELETE_PANTRY_ITEM,
                new DeletePantryItemActionPayload(existing.getId(), existing.getName()));
    }

    private Outcome proposeConsume(Long userId, ChatSession session, AiFunctionCall call) {
        String name = stringArg(call, "name");
        PantryItemNameResolver.Result resolution = nameResolver.resolve(userId, name);
        Outcome clarification = clarificationOrNull(name, resolution);
        if (clarification != null) {
            return clarification;
        }
        PantryItem existing = resolution.item();
        BigDecimal amount = bigDecimalArg(call, "quantity", null);
        if (amount == null || amount.signum() <= 0) {
            log.warn("consume_pantry_item missing/invalid quantity: {}", call.args());
            return Outcome.none();
        }
        return persist(session, ChatActionType.CONSUME_PANTRY_ITEM,
                new ConsumePantryItemActionPayload(
                        existing.getId(), existing.getName(), amount,
                        existing.getUnit(), existing.getQuantity()));
    }

    private Outcome clarificationOrNull(String requestedName, PantryItemNameResolver.Result r) {
        return switch (r.outcome()) {
            case FOUND -> null;
            case NOT_FOUND -> Outcome.clarify(
                    "I don't see \"" + requestedName + "\" in your pantry. "
                            + "Which item did you mean?");
            case AMBIGUOUS -> Outcome.clarify(
                    "You have multiple items called \"" + requestedName + "\": "
                            + r.candidates().stream()
                                    .map(i -> i.getQuantity() + " " + i.getUnit())
                                    .collect(Collectors.joining(", "))
                            + ". Which one?");
        };
    }

    private Outcome persist(ChatSession session, ChatActionType type, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            log.warn("Failed to serialize chat action payload for {}", type, ex);
            return Outcome.none();
        }
        ChatAction action = actionService.propose(session, type, json);
        return Outcome.proposed(new ProposedActionResponse(
                action.getId(), action.getType(), action.getStatus(), payload));
    }

    private <T> T convert(AiFunctionCall call, Class<T> type) {
        try {
            return objectMapper.convertValue(call.args(), type);
        } catch (IllegalArgumentException ex) {
            log.warn("Model returned unusable {} args: {} — {}", call.name(), call.args(), ex.getMessage());
            return null;
        }
    }

    private static String stringArg(AiFunctionCall call, String key) {
        Object v = call.args().get(key);
        return v == null ? null : v.toString();
    }

    private static String stringArg(AiFunctionCall call, String key, String fallback) {
        Object v = call.args().get(key);
        return v == null ? fallback : v.toString();
    }

    private static String nullableStringArg(AiFunctionCall call, String key, String fallback) {
        Object v = call.args().get(key);
        if (v == null) {
            return fallback;
        }
        String s = v.toString();
        return s.isBlank() ? fallback : s;
    }

    private static BigDecimal bigDecimalArg(AiFunctionCall call, String key, BigDecimal fallback) {
        Object v = call.args().get(key);
        if (v == null) {
            return fallback;
        }
        if (v instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        try {
            return new BigDecimal(v.toString());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static java.time.LocalDate nullableLocalDateArg(
            AiFunctionCall call, String key, String fallback) {
        Object v = call.args().get(key);
        String raw = v == null ? fallback : v.toString();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(raw);
        } catch (java.time.format.DateTimeParseException ex) {
            return null;
        }
    }
}
