package org.example.pantrypilot.controller;

import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.dto.ConsumeQuantityRequest;
import org.example.pantrypilot.dto.CreatePantryItemRequest;
import org.example.pantrypilot.dto.PantryItemResponse;
import org.example.pantrypilot.dto.UpdatePantryItemRequest;
import org.example.pantrypilot.service.PantryItemService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public PantryItemResponse create(@Valid @RequestBody CreatePantryItemRequest request) {
        return pantryItemService.createItem(currentUserId(), request);
    }

    @GetMapping
    public List<PantryItemResponse> list() {
        return pantryItemService.listItems(currentUserId());
    }

    @GetMapping("/expiring")
    public List<PantryItemResponse> listExpiring(
            @RequestParam(name = "days", required = false) Integer days) {
        return pantryItemService.listExpiringItems(currentUserId(), days);
    }

    @GetMapping("/{id}")
    public PantryItemResponse get(@PathVariable Long id) {
        return pantryItemService.getItem(currentUserId(), id);
    }

    @PutMapping("/{id}")
    public PantryItemResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePantryItemRequest request) {
        return pantryItemService.updateItem(currentUserId(), id, request);
    }

    @PatchMapping("/{id}/consume")
    public PantryItemResponse consume(
            @PathVariable Long id,
            @Valid @RequestBody ConsumeQuantityRequest request) {
        return pantryItemService.consumeQuantity(currentUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        pantryItemService.deleteItem(currentUserId(), id);
    }

    private Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (Long) principal;
    }
}
