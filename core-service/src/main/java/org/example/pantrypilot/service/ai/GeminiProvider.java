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
    public static final String TOOL_BULK_DELETE_PANTRY_ITEMS = "bulk_delete_pantry_items";
    public static final String TOOL_CREATE_SHOPPING_LIST = "create_shopping_list";
    public static final String TOOL_ADD_SHOPPING_LIST_ITEM = "add_shopping_list_item";
    public static final String TOOL_REMOVE_SHOPPING_LIST_ITEM = "remove_shopping_list_item";
    public static final String TOOL_CHECK_SHOPPING_LIST_ITEM = "check_shopping_list_item";
    public static final String TOOL_UNCHECK_SHOPPING_LIST_ITEM = "uncheck_shopping_list_item";
    public static final String TOOL_GENERATE_SHOPPING_LIST_FROM_RECIPE = "generate_shopping_list_from_recipe";
    public static final String TOOL_CREATE_RECIPE = "create_recipe";
    public static final String TOOL_DELETE_RECIPE = "delete_recipe";
    public static final String TOOL_ADD_RECIPE_INGREDIENT = "add_recipe_ingredient";
    public static final String TOOL_REMOVE_RECIPE_INGREDIENT = "remove_recipe_ingredient";

    private static final List<Map<String, Object>> TOOL_DECLARATIONS = List.of(Map.of(
            "functionDeclarations", ToolDeclarations.all()));

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
