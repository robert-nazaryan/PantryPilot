package org.example.pantrypilot.service.ai;

import org.example.pantrypilot.config.AiProperties;
import org.example.pantrypilot.config.GeminiProperties;
import org.example.pantrypilot.config.GroqProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderSelectionTest {

    private static final String[] BASE_PROPS = {
            "ai.chat.enabled=true",
            "gemini.api-key=fake",
            "gemini.model=gemini-x",
            "gemini.api-base-url=https://example.invalid",
            "groq.api-key=fake",
            "groq.model=openai/gpt-oss-120b",
            "groq.api-base-url=https://api.groq.com/openai/v1"
    };

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
            .withUserConfiguration(AiPropsConfig.class, GeminiProvider.class, GroqProvider.class);

    @Test
    void defaultProvider_wiresGeminiProviderAndNoGroq() {
        contextRunner
                .withPropertyValues(BASE_PROPS)
                .run(context -> {
                    assertThat(context).hasSingleBean(AiProvider.class);
                    assertThat(context.getBean(AiProvider.class)).isInstanceOf(GeminiProvider.class);
                    assertThat(context.getBeansOfType(GroqProvider.class)).isEmpty();
                });
    }

    @Test
    void explicitGeminiProvider_wiresGeminiProviderAndNoGroq() {
        contextRunner
                .withPropertyValues(BASE_PROPS)
                .withPropertyValues("ai.provider=gemini")
                .run(context -> {
                    assertThat(context).hasSingleBean(AiProvider.class);
                    assertThat(context.getBean(AiProvider.class)).isInstanceOf(GeminiProvider.class);
                    assertThat(context.getBeansOfType(GroqProvider.class)).isEmpty();
                });
    }

    @Test
    void groqProvider_wiresGroqProviderAndNoGemini() {
        contextRunner
                .withPropertyValues(BASE_PROPS)
                .withPropertyValues("ai.provider=groq")
                .run(context -> {
                    assertThat(context).hasSingleBean(AiProvider.class);
                    assertThat(context.getBean(AiProvider.class)).isInstanceOf(GroqProvider.class);
                    assertThat(context.getBeansOfType(GeminiProvider.class)).isEmpty();
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({AiProperties.class, GeminiProperties.class, GroqProperties.class})
    static class AiPropsConfig {
    }
}
