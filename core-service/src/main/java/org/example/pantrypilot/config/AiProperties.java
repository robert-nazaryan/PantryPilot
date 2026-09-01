package org.example.pantrypilot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.chat")
public record AiProperties(
        boolean enabled
) {
}
