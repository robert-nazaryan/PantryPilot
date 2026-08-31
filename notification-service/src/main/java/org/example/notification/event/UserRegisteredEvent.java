package org.example.notification.event;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID eventId,
        Instant occurredAt,
        Long userId,
        String email,
        String displayName
) {
}
