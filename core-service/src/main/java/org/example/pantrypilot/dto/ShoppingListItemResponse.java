package org.example.pantrypilot.dto;

import org.example.pantrypilot.model.ShoppingListItem;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ShoppingListItemResponse(
        Long id,
        String name,
        BigDecimal quantity,
        String unit,
        boolean checked,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static ShoppingListItemResponse from(ShoppingListItem item) {
        return new ShoppingListItemResponse(
                item.getId(),
                item.getName(),
                item.getQuantity(),
                item.getUnit(),
                item.isChecked(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
