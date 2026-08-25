package org.example.pantrypilot.controller;

import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.dto.ConsumeQuantityRequest;
import org.example.pantrypilot.dto.CreatePantryItemRequest;
import org.example.pantrypilot.dto.PantryItemResponse;
import org.example.pantrypilot.dto.UpdatePantryItemRequest;
import org.example.pantrypilot.security.CurrentUserId;
import org.example.pantrypilot.service.PantryItemService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pantry-items")
@RequiredArgsConstructor
public class PantryItemController {

    private final PantryItemService pantryItemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PantryItemResponse create(
            @CurrentUserId Long userId,
            @Valid @RequestBody CreatePantryItemRequest request) {
        return pantryItemService.createItem(userId, request);
    }

    @GetMapping
    public List<PantryItemResponse> list(@CurrentUserId Long userId) {
        return pantryItemService.listItems(userId);
    }

    @GetMapping("/expiring")
    public List<PantryItemResponse> listExpiring(
            @CurrentUserId Long userId,
            @RequestParam(name = "days", required = false) Integer days) {
        return pantryItemService.listExpiringItems(userId, days);
    }

    @GetMapping("/{id}")
    public PantryItemResponse get(@CurrentUserId Long userId, @PathVariable Long id) {
        return pantryItemService.getItem(userId, id);
    }

    @PutMapping("/{id}")
    public PantryItemResponse update(
            @CurrentUserId Long userId,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePantryItemRequest request) {
        return pantryItemService.updateItem(userId, id, request);
    }

    @PatchMapping("/{id}/consume")
    public PantryItemResponse consume(
            @CurrentUserId Long userId,
            @PathVariable Long id,
            @Valid @RequestBody ConsumeQuantityRequest request) {
        return pantryItemService.consumeQuantity(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId Long userId, @PathVariable Long id) {
        pantryItemService.deleteItem(userId, id);
    }
}
