package org.example.pantrypilot.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record ConsumeQuantityRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 9, fraction = 3) BigDecimal quantity
) {}
