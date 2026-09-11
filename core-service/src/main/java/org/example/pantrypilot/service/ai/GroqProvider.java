package org.example.pantrypilot.service.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.pantrypilot.config.AiProperties;
import org.example.pantrypilot.config.GroqProperties;
import org.example.pantrypilot.model.ChatRole;
import org.example.pantrypilot.service.exception.AiUnavailableException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "groq")
public class GroqProvider implements AiProvider {

    private static final String ROLE_SYSTEM = "system";
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    private static final List<Map<String, Object>> TOOL_DECLARATIONS = AiTools.functionSchemas().stream()
            .<Map<String, Object>>map(fn -> Map.of("type", "function", "function", fn))
            .toList();

    private static final TypeReference<Map<String, Object>> STRING_OBJECT_MAP = new TypeReference<>() { };

    private final AiProperties aiProperties;
    private final GroqProperties groqProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GroqProvider(AiProperties aiProperties, GroqProperties groqProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.groqProperties = groqProperties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(groqProperties.apiBaseUrl())
                .build();
    }

    @Override
    public boolean isAvailable() {
        return aiProperties.enabled() && groqProperties.isConfigured();
    }

    @Override
    public AiResponse chat(String systemContext, List<AiChatTurn> history, String userMessage) {
        if (!isAvailable()) {
            throw new AiUnavailableException("AI chat is disabled or GROQ_API_KEY is not configured");
        }
        Map<String, Object> payload = buildPayload(systemContext, history, userMessage);
        Map<String, Object> response;
        try {
            response = TransientAiRetry.call(() -> callGroq(payload));
        } catch (RestClientException ex) {
            throw new AiUnavailableException("Groq call failed: " + ex.getMessage(), ex);
        }
        return extractResponse(response, objectMapper);
    }

    private Map<String, Object> callGroq(Map<String, Object> payload) {
        return restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + groqProperties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() { });
    }

    private Map<String, Object> buildPayload(
            String systemContext, List<AiChatTurn> history, String userMessage) {
        List<Map<String, Object>> messages = new ArrayList<>();
        if (systemContext != null && !systemContext.isBlank()) {
            messages.add(Map.of("role", ROLE_SYSTEM, "content", systemContext));
        }
        for (AiChatTurn turn : history) {
            messages.add(Map.of(
                    "role", turn.role() == ChatRole.USER ? ROLE_USER : ROLE_ASSISTANT,
                    "content", turn.content()));
        }
        messages.add(Map.of("role", ROLE_USER, "content", userMessage));

        Map<String, Object> body = new HashMap<>();
        body.put("model", groqProperties.model());
        body.put("messages", messages);
        body.put("tools", TOOL_DECLARATIONS);
        return body;
    }

    @SuppressWarnings("unchecked")
    static AiResponse extractResponse(Map<String, Object> response, ObjectMapper objectMapper) {
        if (response == null) {
            throw new AiUnavailableException("Groq returned an empty response");
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new AiUnavailableException("Groq returned no choices");
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) {
            throw new AiUnavailableException("Groq choice had no message");
        }
        String text = messageText(message.get("content"));
        AiFunctionCall functionCall = firstToolCall(message.get("tool_calls"), objectMapper);
        if (text.isEmpty() && functionCall == null) {
            throw new AiUnavailableException("Groq returned neither text nor tool call");
        }
        return new AiResponse(text, functionCall);
    }

    private static String messageText(Object rawContent) {
        if (rawContent == null) {
            return "";
        }
        if (rawContent instanceof String s) {
            return s.trim();
        }
        if (rawContent instanceof List<?> parts) {
            StringBuilder sb = new StringBuilder();
            for (Object part : parts) {
                if (part instanceof Map<?, ?> map && map.get("text") instanceof String s) {
                    sb.append(s);
                }
            }
            return sb.toString().trim();
        }
        return rawContent.toString().trim();
    }

    private static AiFunctionCall firstToolCall(Object rawToolCalls, ObjectMapper objectMapper) {
        if (!(rawToolCalls instanceof List<?> calls)) {
            return null;
        }
        return calls.stream()
                .map(GroqProvider::asFunctionMap)
                .filter(fn -> fn != null && fn.get("name") instanceof String)
                .findFirst()
                .map(fn -> new AiFunctionCall(
                        (String) fn.get("name"),
                        parseArgs(fn.get("arguments"), objectMapper)))
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asFunctionMap(Object rawCall) {
        if (!(rawCall instanceof Map<?, ?> callMap)) {
            return null;
        }
        Object fnObj = callMap.get("function");
        return fnObj instanceof Map<?, ?> ? (Map<String, Object>) fnObj : null;
    }

    private static Map<String, Object> parseArgs(Object rawArgs, ObjectMapper objectMapper) {
        if (rawArgs instanceof Map<?, ?> alreadyParsed) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cast = (Map<String, Object>) alreadyParsed;
            return cast;
        }
        if (!(rawArgs instanceof String raw) || raw.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(raw, STRING_OBJECT_MAP);
            return parsed == null ? Map.of() : parsed;
        } catch (JacksonException ex) {
            log.warn("Failed to parse Groq tool_call arguments as JSON: {}", raw, ex);
            return Map.of();
        }
    }
}
