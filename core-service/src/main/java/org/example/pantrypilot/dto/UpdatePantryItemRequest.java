package org.example.pantrypilot.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdatePantryItemRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) @Digits(integer = 9, fraction = 3) BigDecimal quantity,
        @NotBlank @Size(max = 30) String unit,
        @Size(max = 50) String category,
        LocalDate expiryDate
) {
}
