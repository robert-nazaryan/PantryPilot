package org.example.pantrypilot.event;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID eventId,
        Instant occurredAt,
        Long userId,
        String email,
        String displayName
) {

    public static UserRegisteredEvent now(Long userId, String email, String displayName) {
        return new UserRegisteredEvent(UUID.randomUUID(), Instant.now(), userId, email, displayName);
    }
}
