package org.example.pantrypilot.service.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.ObjectMapper;
import org.example.pantrypilot.config.AiProperties;
import org.example.pantrypilot.config.GroqProperties;
import org.example.pantrypilot.service.exception.AiUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroqProviderTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void isAvailable_returnsFalseWhenChatDisabled() {
        GroqProvider provider = new GroqProvider(
                new AiProperties(false),
                new GroqProperties("secret", "openai/gpt-oss-120b", "https://api.groq.com/openai/v1"),
                objectMapper);

        assertThat(provider.isAvailable()).isFalse();
    }

    @Test
    void isAvailable_returnsFalseWhenApiKeyBlank() {
        GroqProvider provider = new GroqProvider(
                new AiProperties(true),
                new GroqProperties("", "openai/gpt-oss-120b", "https://api.groq.com/openai/v1"),
                objectMapper);

        assertThat(provider.isAvailable()).isFalse();
    }

    @Test
    void isAvailable_returnsTrueWhenEnabledAndConfigured() {
        GroqProvider provider = new GroqProvider(
                new AiProperties(true),
                new GroqProperties("secret", "openai/gpt-oss-120b", "https://api.groq.com/openai/v1"),
                objectMapper);

        assertThat(provider.isAvailable()).isTrue();
    }

    @Test
    void chat_whenDisabled_throwsAiUnavailable() {
        GroqProvider provider = new GroqProvider(
                new AiProperties(false),
                new GroqProperties("secret", "openai/gpt-oss-120b", "https://api.groq.com/openai/v1"),
                objectMapper);

        assertThatThrownBy(() -> provider.chat("ctx", List.of(), "hi"))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void extractResponse_parsesPlainTextReply() {
        Map<String, Object> response = Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of(
                                "role", "assistant",
                                "content", "Try a stir-fry."))));

        AiResponse out = GroqProvider.extractResponse(response, objectMapper);

        assertThat(out.text()).isEqualTo("Try a stir-fry.");
        assertThat(out.hasFunctionCall()).isFalse();
    }

    @Test
    void extractResponse_parsesToolCallWithJsonStringArguments() {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "assistant");
        message.put("content", null);
        message.put("tool_calls", List.of(Map.of(
                "id", "call_abc",
                "type", "function",
                "function", Map.of(
                        "name", AiTools.TOOL_CREATE_PANTRY_ITEM,
                        "arguments", "{\"name\":\"Milk\",\"quantity\":2,\"unit\":\"L\"}"))));
        Map<String, Object> response = Map.of("choices", List.of(Map.of("message", message)));

        AiResponse out = GroqProvider.extractResponse(response, objectMapper);

        assertThat(out.hasFunctionCall()).isTrue();
        AiFunctionCall call = out.functionCall();
        assertThat(call.name()).isEqualTo(AiTools.TOOL_CREATE_PANTRY_ITEM);
        assertThat(call.args())
                .containsEntry("name", "Milk")
                .containsEntry("unit", "L")
                .containsEntry("quantity", 2);
    }

    @Test
    void extractResponse_parsesToolCallAlongsideTextContent() {
        Map<String, Object> response = Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of(
                                "role", "assistant",
                                "content", "Adding milk.",
                                "tool_calls", List.of(Map.of(
                                        "type", "function",
                                        "function", Map.of(
                                                "name", AiTools.TOOL_CREATE_PANTRY_ITEM,
                                                "arguments", "{\"name\":\"Milk\"}")))))));

        AiResponse out = GroqProvider.extractResponse(response, objectMapper);

        assertThat(out.text()).isEqualTo("Adding milk.");
        assertThat(out.hasFunctionCall()).isTrue();
        assertThat(out.functionCall().name()).isEqualTo(AiTools.TOOL_CREATE_PANTRY_ITEM);
    }

    @Test
    void extractResponse_picksFirstToolCallWhenModelReturnsSeveral() {
        Map<String, Object> message = new HashMap<>();
        message.put("content", null);
        message.put("tool_calls", List.of(
                Map.of("type", "function", "function", Map.of(
                        "name", AiTools.TOOL_CREATE_PANTRY_ITEM,
                        "arguments", "{\"name\":\"Milk\"}")),
                Map.of("type", "function", "function", Map.of(
                        "name", AiTools.TOOL_DELETE_PANTRY_ITEM,
                        "arguments", "{\"name\":\"Milk\"}"))));
        Map<String, Object> response = Map.of("choices", List.of(Map.of("message", message)));

        AiResponse out = GroqProvider.extractResponse(response, objectMapper);

        assertThat(out.functionCall().name()).isEqualTo(AiTools.TOOL_CREATE_PANTRY_ITEM);
    }

    @Test
    void extractResponse_toolCallWithBlankArguments_returnsEmptyArgMap() {
        Map<String, Object> message = new HashMap<>();
        message.put("content", null);
        message.put("tool_calls", List.of(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", AiTools.TOOL_CREATE_SHOPPING_LIST,
                        "arguments", ""))));
        Map<String, Object> response = Map.of("choices", List.of(Map.of("message", message)));

        AiResponse out = GroqProvider.extractResponse(response, objectMapper);

        assertThat(out.functionCall().name()).isEqualTo(AiTools.TOOL_CREATE_SHOPPING_LIST);
        assertThat(out.functionCall().args()).isEmpty();
    }

    @Test
    void extractResponse_toolCallWithMalformedArguments_returnsEmptyArgMap() {
        Map<String, Object> message = new HashMap<>();
        message.put("content", null);
        message.put("tool_calls", List.of(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", AiTools.TOOL_CREATE_PANTRY_ITEM,
                        "arguments", "not-json"))));
        Map<String, Object> response = Map.of("choices", List.of(Map.of("message", message)));

        AiResponse out = GroqProvider.extractResponse(response, objectMapper);

        assertThat(out.functionCall().name()).isEqualTo(AiTools.TOOL_CREATE_PANTRY_ITEM);
        assertThat(out.functionCall().args()).isEmpty();
    }

    @Test
    void extractResponse_nullResponse_throwsAiUnavailable() {
        assertThatThrownBy(() -> GroqProvider.extractResponse(null, objectMapper))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void extractResponse_emptyChoices_throwsAiUnavailable() {
        Map<String, Object> response = Map.of("choices", List.of());

        assertThatThrownBy(() -> GroqProvider.extractResponse(response, objectMapper))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void extractResponse_missingMessage_throwsAiUnavailable() {
        Map<String, Object> response = Map.of("choices", List.of(Map.of()));

        assertThatThrownBy(() -> GroqProvider.extractResponse(response, objectMapper))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void extractResponse_emptyContentAndNoToolCalls_throwsAiUnavailable() {
        Map<String, Object> response = Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("role", "assistant", "content", ""))));

        assertThatThrownBy(() -> GroqProvider.extractResponse(response, objectMapper))
                .isInstanceOf(AiUnavailableException.class);
    }
}
