package org.example.pantrypilot.service.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.example.pantrypilot.config.AiProperties;
import org.example.pantrypilot.config.GeminiProperties;
import org.example.pantrypilot.model.ChatRole;
import org.example.pantrypilot.service.exception.AiUnavailableException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class GeminiProvider implements AiProvider {

    private static final String GEMINI_ROLE_USER = "user";
    private static final String GEMINI_ROLE_MODEL = "model";

    public static final String TOOL_CREATE_PANTRY_ITEM = "create_pantry_item";
    public static final String TOOL_UPDATE_PANTRY_ITEM = "update_pantry_item";
    public static final String TOOL_DELETE_PANTRY_ITEM = "delete_pantry_item";
    public static final String TOOL_CONSUME_PANTRY_ITEM = "consume_pantry_item";

    private static final List<Map<String, Object>> TOOL_DECLARATIONS = List.of(Map.of(
            "functionDeclarations", List.of(
                    createPantryItemDeclaration(),
                    updatePantryItemDeclaration(),
                    deletePantryItemDeclaration(),
                    consumePantryItemDeclaration())));

    private final AiProperties aiProperties;
    private final GeminiProperties geminiProperties;
    private final RestClient restClient;

    public GeminiProvider(AiProperties aiProperties, GeminiProperties geminiProperties) {
        this.aiProperties = aiProperties;
        this.geminiProperties = geminiProperties;
        this.restClient = RestClient.builder()
                .baseUrl(geminiProperties.apiBaseUrl())
                .build();
    }

    @Override
    public boolean isAvailable() {
        return aiProperties.enabled() && geminiProperties.isConfigured();
    }

    @Override
    public AiResponse chat(String systemContext, List<AiChatTurn> history, String userMessage) {
        if (!isAvailable()) {
            throw new AiUnavailableException("AI chat is disabled or GEMINI_API_KEY is not configured");
        }
        Map<String, Object> payload = buildPayload(systemContext, history, userMessage);
        Map<String, Object> response;
        try {
            response = TransientGeminiRetry.call(() -> callGemini(payload));
        } catch (RestClientException ex) {
            throw new AiUnavailableException("Gemini call failed: " + ex.getMessage(), ex);
        }
        return extractResponse(response);
    }

    private Map<String, Object> callGemini(Map<String, Object> payload) {
        return restClient.post()
                .uri("/v1beta/models/{model}:generateContent?key={key}",
                        geminiProperties.model(), geminiProperties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() { });
    }

    private static Map<String, Object> createPantryItemDeclaration() {
        return Map.of(
                "name", TOOL_CREATE_PANTRY_ITEM,
                "description",
                "Propose adding a single item to the user's pantry. Use this ONLY when the user "
                        + "clearly asks to add, save, log, or track a pantry item. Do not use it for "
                        + "suggestions or hypothetical items. The user will see a confirmation card "
                        + "and must click Confirm before the item is actually created.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "name", Map.of("type", "string",
                                        "description", "Name of the item, e.g. 'Milk' or 'Whole wheat flour'."),
                                "quantity", Map.of("type", "number",
                                        "description", "Positive numeric quantity, e.g. 2 or 0.5."),
                                "unit", Map.of("type", "string",
                                        "description",
                                        "Unit of measurement. Common values: pcs, g, kg, ml, l, tsp, tbsp, cup, oz, lb."),
                                "category", Map.of("type", "string",
                                        "description",
                                        "Optional category. Common values: dairy, produce, meat, grains, spices, frozen, bakery, other."),
                                "expiryDate", Map.of("type", "string",
                                        "description",
                                        "Optional expiry date in ISO format YYYY-MM-DD. Only include if the user gave one explicitly.")),
                        "required", List.of("name", "quantity", "unit")));
    }

    private static Map<String, Object> updatePantryItemDeclaration() {
        return Map.of(
                "name", TOOL_UPDATE_PANTRY_ITEM,
                "description",
                "Propose updating fields on an existing pantry item the user already owns. Use ONLY "
                        + "when the user asks to change/rename/edit/set an existing item. Identify the "
                        + "item by its current name (case-insensitive exact match against the pantry "
                        + "listed in the system context). Provide only the fields that should change; "
                        + "omitted fields will keep their current values. The user will see a confirmation "
                        + "card before the change is applied.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "name", Map.of("type", "string",
                                        "description",
                                        "The current name of the pantry item to update (used to identify it)."),
                                "quantity", Map.of("type", "number",
                                        "description",
                                        "New quantity (>= 0). Omit if the user did not ask to change the quantity."),
                                "unit", Map.of("type", "string",
                                        "description",
                                        "New unit of measurement. Omit if the user did not ask to change the unit."),
                                "category", Map.of("type", "string",
                                        "description", "New category. Omit if unchanged."),
                                "expiryDate", Map.of("type", "string",
                                        "description", "New expiry date in ISO YYYY-MM-DD. Omit if unchanged.")),
                        "required", List.of("name")));
    }

    private static Map<String, Object> deletePantryItemDeclaration() {
        return Map.of(
                "name", TOOL_DELETE_PANTRY_ITEM,
                "description",
                "Propose removing an existing pantry item entirely. Use ONLY when the user asks to "
                        + "remove/delete/discard an item. Identify by current name. The user will see a "
                        + "confirmation card before the item is deleted.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "name", Map.of("type", "string",
                                        "description",
                                        "The current name of the pantry item to delete.")),
                        "required", List.of("name")));
    }

    private static Map<String, Object> consumePantryItemDeclaration() {
        return Map.of(
                "name", TOOL_CONSUME_PANTRY_ITEM,
                "description",
                "Propose reducing the quantity of an existing pantry item (partial consumption). Use "
                        + "when the user says they used/ate/drank/consumed part of an item. Identify by "
                        + "current name. The 'quantity' argument is how much to consume, not the "
                        + "remaining amount. Do not use for full removal — use " + TOOL_DELETE_PANTRY_ITEM
                        + " for that. The user will see a confirmation card before the change is applied.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "name", Map.of("type", "string",
                                        "description",
                                        "The current name of the pantry item to consume from."),
                                "quantity", Map.of("type", "number",
                                        "description",
                                        "How much to subtract from the current quantity (positive, must not exceed available).")),
                        "required", List.of("name", "quantity")));
    }

    private static Map<String, Object> buildPayload(
            String systemContext, List<AiChatTurn> history, String userMessage) {
        List<Map<String, Object>> contents = new ArrayList<>();
        for (AiChatTurn turn : history) {
            contents.add(Map.of(
                    "role", turn.role() == ChatRole.USER ? GEMINI_ROLE_USER : GEMINI_ROLE_MODEL,
                    "parts", List.of(Map.of("text", turn.content()))));
        }
        contents.add(Map.of(
                "role", GEMINI_ROLE_USER,
                "parts", List.of(Map.of("text", userMessage))));

        Map<String, Object> body = new HashMap<>();
        body.put("contents", contents);
        body.put("tools", TOOL_DECLARATIONS);
        if (systemContext != null && !systemContext.isBlank()) {
            body.put("systemInstruction", Map.of(
                    "parts", List.of(Map.of("text", systemContext))));
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    static AiResponse extractResponse(Map<String, Object> response) {
        if (response == null) {
            throw new AiUnavailableException("Gemini returned an empty response");
        }
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new AiUnavailableException("Gemini returned no candidates");
        }
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        if (content == null) {
            throw new AiUnavailableException("Gemini candidate had no content");
        }
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            throw new AiUnavailableException("Gemini candidate content had no parts");
        }
        StringBuilder text = new StringBuilder();
        AiFunctionCall functionCall = null;
        for (Map<String, Object> part : parts) {
            Object partText = part.get("text");
            if (partText != null) {
                text.append(partText);
            }
            Object rawFn = part.get("functionCall");
            if (rawFn instanceof Map<?, ?> fnMap && functionCall == null) {
                Object name = fnMap.get("name");
                Object args = fnMap.get("args");
                if (name instanceof String fnName) {
                    Map<String, Object> argMap = args instanceof Map<?, ?>
                            ? (Map<String, Object>) args : Map.of();
                    functionCall = new AiFunctionCall(fnName, argMap);
                }
            }
        }
        String trimmed = text.toString().trim();
        if (trimmed.isEmpty() && functionCall == null) {
            throw new AiUnavailableException("Gemini returned neither text nor function call");
        }
        return new AiResponse(trimmed, functionCall);
    }
}
