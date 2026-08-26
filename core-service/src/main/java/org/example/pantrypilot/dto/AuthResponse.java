package org.example.pantrypilot.dto;

public record AuthResponse(
        String accessToken,
        long expiresIn
) {
}
