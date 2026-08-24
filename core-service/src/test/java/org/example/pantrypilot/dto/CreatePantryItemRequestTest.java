package org.example.pantrypilot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreatePantryItemRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void pastExpiryDate_isRejected() {
        CreatePantryItemRequest req = new CreatePantryItemRequest(
                "Milk", new BigDecimal("1.0"), "L", null, LocalDate.now().minusDays(1));

        Set<ConstraintViolation<CreatePantryItemRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("expiryDate"));
    }

    @Test
    void todayOrFutureExpiryDate_isAccepted() {
        CreatePantryItemRequest req = new CreatePantryItemRequest(
                "Milk", new BigDecimal("1.0"), "L", null, LocalDate.now());

        assertThat(validator.validate(req)).isEmpty();
    }

    @Test
    void nullExpiryDate_isAccepted() {
        CreatePantryItemRequest req = new CreatePantryItemRequest(
                "Milk", new BigDecimal("1.0"), "L", null, null);

        assertThat(validator.validate(req)).isEmpty();
    }

    @Test
    void blankName_isRejected() {
        CreatePantryItemRequest req = new CreatePantryItemRequest(
                "", new BigDecimal("1.0"), "L", null, null);

        Set<ConstraintViolation<CreatePantryItemRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    void zeroQuantity_isRejected() {
        CreatePantryItemRequest req = new CreatePantryItemRequest(
                "Milk", BigDecimal.ZERO, "L", null, null);

        Set<ConstraintViolation<CreatePantryItemRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("quantity"));
    }
}
