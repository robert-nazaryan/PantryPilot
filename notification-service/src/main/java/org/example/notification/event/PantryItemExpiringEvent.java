package org.example.notification.event;

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

    public record ExpiringItem(
            String name,
            BigDecimal quantity,
            String unit,
            LocalDate expiryDate
    ) {
    }
}
