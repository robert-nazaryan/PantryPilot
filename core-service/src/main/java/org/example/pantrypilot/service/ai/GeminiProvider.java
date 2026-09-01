package org.example.pantrypilot.service.ai;

import java.util.ArrayList;
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
    public String chat(String systemContext, List<AiChatTurn> history, String userMessage) {
        if (!isAvailable()) {
            throw new AiUnavailableException("AI chat is disabled or GEMINI_API_KEY is not configured");
        }
        Map<String, Object> payload = buildPayload(systemContext, history, userMessage);
        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}",
                            geminiProperties.model(), geminiProperties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() { });
        } catch (RestClientException ex) {
            throw new AiUnavailableException("Gemini call failed: " + ex.getMessage(), ex);
        }
        return extractText(response);
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

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("contents", contents);
        if (systemContext != null && !systemContext.isBlank()) {
            body.put("systemInstruction", Map.of(
                    "parts", List.of(Map.of("text", systemContext))));
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private static String extractText(Map<String, Object> response) {
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
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> part : parts) {
            Object text = part.get("text");
            if (text != null) {
                sb.append(text);
            }
        }
        String result = sb.toString().trim();
        if (result.isEmpty()) {
            throw new AiUnavailableException("Gemini returned empty text");
        }
        return result;
    }
}
