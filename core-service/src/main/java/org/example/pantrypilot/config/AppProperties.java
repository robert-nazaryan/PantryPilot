package org.example.pantrypilot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.frontend")
public record AppProperties(
        String url
) {
}
