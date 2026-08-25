package org.example.pantrypilot.dto;

public record ErrorResponse(
        String error,
        String message
) {
}
