package org.example.pantrypilot.controller;

import org.example.pantrypilot.dto.ErrorResponse;
import org.example.pantrypilot.service.exception.InsufficientQuantityException;
import org.example.pantrypilot.service.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PantryItemExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, "not_found", ex.getMessage());
    }

    @ExceptionHandler(InsufficientQuantityException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientQuantity(InsufficientQuantityException ex) {
        return respond(HttpStatus.BAD_REQUEST, "insufficient_quantity", ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message));
    }
}
