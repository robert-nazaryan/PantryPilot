package org.example.pantrypilot.dto;

import java.math.BigDecimal;

public record ConsumePantryItemActionPayload(
        Long itemId,
        String name,
        BigDecimal quantity,
        String unit,
        BigDecimal availableQuantity
) {
}
