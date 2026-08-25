package org.example.pantrypilot.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.dto.CreateShoppingListRequest;
import org.example.pantrypilot.dto.PageResponse;
import org.example.pantrypilot.dto.ShoppingListResponse;
import org.example.pantrypilot.dto.ShoppingListSummaryResponse;
import org.example.pantrypilot.dto.UpdateShoppingListRequest;
import org.example.pantrypilot.security.CurrentUserId;
import org.example.pantrypilot.service.ShoppingListService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shopping-lists")
@RequiredArgsConstructor
public class ShoppingListController {

    private final ShoppingListService shoppingListService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShoppingListResponse create(
            @CurrentUserId Long userId,
            @Valid @RequestBody CreateShoppingListRequest request) {
        return shoppingListService.createList(userId, request);
    }

    @GetMapping
    public PageResponse<ShoppingListSummaryResponse> list(
            @CurrentUserId Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return shoppingListService.listLists(userId, pageable);
    }

    @GetMapping("/{id}")
    public ShoppingListResponse get(@CurrentUserId Long userId, @PathVariable Long id) {
        return shoppingListService.getList(userId, id);
    }

    @PutMapping("/{id}")
    public ShoppingListResponse update(
            @CurrentUserId Long userId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateShoppingListRequest request) {
        return shoppingListService.updateList(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId Long userId, @PathVariable Long id) {
        shoppingListService.deleteList(userId, id);
    }
}
