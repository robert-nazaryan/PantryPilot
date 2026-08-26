package org.example.pantrypilot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.refresh-cookie")
public record AuthCookieProperties(
        boolean secure
) {
}
