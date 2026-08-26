package org.example.pantrypilot.service;

import java.time.Duration;

public record TokenPair(
        String accessToken,
        String rawRefreshToken,
        long accessTokenTtlSeconds,
        Duration refreshTokenTtl
) {
}
