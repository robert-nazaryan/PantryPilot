package org.example.pantrypilot.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
