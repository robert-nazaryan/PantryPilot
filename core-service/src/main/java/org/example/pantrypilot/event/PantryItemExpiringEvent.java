package org.example.pantrypilot.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PantryItemExpiringEvent(
        UUID eventId,
        Instant occurredAt,
        Long userId,
        String email,
        String displayName,
        List<ExpiringItem> items
) {

    public PantryItemExpiringEvent {
        items = List.copyOf(items);
    }

    public record ExpiringItem(
            String name,
            BigDecimal quantity,
            String unit,
            LocalDate expiryDate
    ) {
    }

    public static PantryItemExpiringEvent now(
            Long userId, String email, String displayName, List<ExpiringItem> items) {
        return new PantryItemExpiringEvent(
                UUID.randomUUID(), Instant.now(), userId, email, displayName, items);
    }
}
