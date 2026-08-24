package org.example.pantrypilot.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.dto.CreateRecipeIngredientRequest;
import org.example.pantrypilot.dto.RecipeIngredientResponse;
import org.example.pantrypilot.dto.UpdateRecipeIngredientRequest;
import org.example.pantrypilot.service.RecipeIngredientService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes/{recipeId}/ingredients")
@RequiredArgsConstructor
public class RecipeIngredientController {

    private final RecipeIngredientService recipeIngredientService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeIngredientResponse add(
            @PathVariable Long recipeId,
            @Valid @RequestBody CreateRecipeIngredientRequest request) {
        return recipeIngredientService.addIngredient(currentUserId(), recipeId, request);
    }

    @PutMapping("/{ingredientId}")
    public RecipeIngredientResponse update(
            @PathVariable Long recipeId,
            @PathVariable Long ingredientId,
            @Valid @RequestBody UpdateRecipeIngredientRequest request) {
        return recipeIngredientService.updateIngredient(currentUserId(), recipeId, ingredientId, request);
    }

    @DeleteMapping("/{ingredientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long recipeId,
            @PathVariable Long ingredientId) {
        recipeIngredientService.deleteIngredient(currentUserId(), recipeId, ingredientId);
    }

    private Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (Long) principal;
    }
}
