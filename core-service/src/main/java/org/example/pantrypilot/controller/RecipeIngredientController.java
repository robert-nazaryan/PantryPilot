package org.example.pantrypilot.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.dto.CreateRecipeIngredientRequest;
import org.example.pantrypilot.dto.RecipeIngredientResponse;
import org.example.pantrypilot.dto.UpdateRecipeIngredientRequest;
import org.example.pantrypilot.security.CurrentUserId;
import org.example.pantrypilot.service.RecipeIngredientService;
import org.springframework.http.HttpStatus;
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
            @CurrentUserId Long userId,
            @PathVariable Long recipeId,
            @Valid @RequestBody CreateRecipeIngredientRequest request) {
        return recipeIngredientService.addIngredient(userId, recipeId, request);
    }

    @PutMapping("/{ingredientId}")
    public RecipeIngredientResponse update(
            @CurrentUserId Long userId,
            @PathVariable Long recipeId,
            @PathVariable Long ingredientId,
            @Valid @RequestBody UpdateRecipeIngredientRequest request) {
        return recipeIngredientService.updateIngredient(userId, recipeId, ingredientId, request);
    }

    @DeleteMapping("/{ingredientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @CurrentUserId Long userId,
            @PathVariable Long recipeId,
            @PathVariable Long ingredientId) {
        recipeIngredientService.deleteIngredient(userId, recipeId, ingredientId);
    }
}
