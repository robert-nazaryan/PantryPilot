package org.example.pantrypilot.dto;

import java.math.BigDecimal;

public record AddShoppingListItemActionPayload(
        Long listId,
        String listName,
        String name,
        BigDecimal quantity,
        String unit
) {
}
