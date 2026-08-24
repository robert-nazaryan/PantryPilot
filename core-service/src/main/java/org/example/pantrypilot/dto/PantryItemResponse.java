package org.example.pantrypilot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.example.pantrypilot.model.PantryItem;

public record PantryItemResponse(
        Long id,
        String name,
        BigDecimal quantity,
        String unit,
        String category,
        LocalDate expiryDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static PantryItemResponse from(PantryItem item) {
        return new PantryItemResponse(
                item.getId(),
                item.getName(),
                item.getQuantity(),
                item.getUnit(),
                item.getCategory(),
                item.getExpiryDate(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
