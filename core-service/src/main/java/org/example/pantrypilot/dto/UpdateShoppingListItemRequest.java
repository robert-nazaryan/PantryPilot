package org.example.pantrypilot.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateShoppingListItemRequest(
        @NotBlank @Size(max = 200) String name,
        @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 9, fraction = 3) BigDecimal quantity,
        @Size(max = 30) String unit,
        @NotNull Boolean checked
) {
}
