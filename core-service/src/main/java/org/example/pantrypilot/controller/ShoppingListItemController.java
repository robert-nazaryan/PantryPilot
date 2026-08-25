package org.example.pantrypilot.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.dto.CreateShoppingListItemRequest;
import org.example.pantrypilot.dto.ShoppingListItemResponse;
import org.example.pantrypilot.dto.ToggleShoppingListItemCheckedRequest;
import org.example.pantrypilot.dto.UpdateShoppingListItemRequest;
import org.example.pantrypilot.security.CurrentUserId;
import org.example.pantrypilot.service.ShoppingListItemService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shopping-lists/{listId}/items")
@RequiredArgsConstructor
public class ShoppingListItemController {

    private final ShoppingListItemService shoppingListItemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShoppingListItemResponse add(
            @CurrentUserId Long userId,
            @PathVariable Long listId,
            @Valid @RequestBody CreateShoppingListItemRequest request) {
        return shoppingListItemService.addItem(userId, listId, request);
    }

    @PutMapping("/{itemId}")
    public ShoppingListItemResponse update(
            @CurrentUserId Long userId,
            @PathVariable Long listId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateShoppingListItemRequest request) {
        return shoppingListItemService.updateItem(userId, listId, itemId, request);
    }

    @PatchMapping("/{itemId}/check")
    public ShoppingListItemResponse setChecked(
            @CurrentUserId Long userId,
            @PathVariable Long listId,
            @PathVariable Long itemId,
            @Valid @RequestBody ToggleShoppingListItemCheckedRequest request) {
        return shoppingListItemService.setChecked(userId, listId, itemId, request);
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @CurrentUserId Long userId,
            @PathVariable Long listId,
            @PathVariable Long itemId) {
        shoppingListItemService.deleteItem(userId, listId, itemId);
    }
}
