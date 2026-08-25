package org.example.pantrypilot.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.dto.CreateRecipeRequest;
import org.example.pantrypilot.dto.PageResponse;
import org.example.pantrypilot.dto.RecipeResponse;
import org.example.pantrypilot.dto.RecipeSummaryResponse;
import org.example.pantrypilot.dto.UpdateRecipeRequest;
import org.example.pantrypilot.security.CurrentUserId;
import org.example.pantrypilot.service.RecipeService;
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
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeResponse create(
            @CurrentUserId Long userId,
            @Valid @RequestBody CreateRecipeRequest request) {
        return recipeService.createRecipe(userId, request);
    }

    @GetMapping
    public PageResponse<RecipeSummaryResponse> list(
            @CurrentUserId Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return recipeService.listRecipes(userId, pageable);
    }

    @GetMapping("/{id}")
    public RecipeResponse get(@CurrentUserId Long userId, @PathVariable Long id) {
        return recipeService.getRecipe(userId, id);
    }

    @PutMapping("/{id}")
    public RecipeResponse update(
            @CurrentUserId Long userId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRecipeRequest request) {
        return recipeService.updateRecipe(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId Long userId, @PathVariable Long id) {
        recipeService.deleteRecipe(userId, id);
    }
}
