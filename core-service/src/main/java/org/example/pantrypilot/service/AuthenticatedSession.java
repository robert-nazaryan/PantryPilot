package org.example.pantrypilot.service;

public record AuthenticatedSession(
        TokenPair pair,
        String displayName
) {
}
