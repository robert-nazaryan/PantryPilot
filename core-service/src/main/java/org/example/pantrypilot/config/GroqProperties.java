package org.example.pantrypilot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "groq")
public record GroqProperties(
        String apiKey,
        String model,
        String apiBaseUrl
) {

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
