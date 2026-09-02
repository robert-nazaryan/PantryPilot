package org.example.pantrypilot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdatePantryItemActionPayload(
        Long itemId,
        String name,
        BigDecimal quantity,
        String unit,
        String category,
        LocalDate expiryDate
) {
}
