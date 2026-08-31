package org.example.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification")
public record MailProperties(
        String fromEmail
) {

    public boolean isConfigured() {
        return fromEmail != null && !fromEmail.isBlank();
    }
}
