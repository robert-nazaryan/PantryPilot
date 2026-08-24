package org.example.pantrypilot.service;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.dto.CreateRecipeRequest;
import org.example.pantrypilot.dto.PageResponse;
import org.example.pantrypilot.dto.RecipeResponse;
import org.example.pantrypilot.dto.RecipeSummaryResponse;
import org.example.pantrypilot.dto.UpdateRecipeRequest;
import org.example.pantrypilot.model.Recipe;
import org.example.pantrypilot.model.User;
import org.example.pantrypilot.repository.RecipeRepository;
import org.example.pantrypilot.repository.UserRepository;
import org.example.pantrypilot.service.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecipeService {

    static final int MAX_PAGE_SIZE = 100;
    static final int DEFAULT_PAGE_SIZE = 20;

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    @Transactional
    public RecipeResponse createRecipe(Long userId, CreateRecipeRequest req) {
        User owner = userRepository.getReferenceById(userId);
        Recipe recipe = Recipe.builder()
                .user(owner)
                .title(req.title())
                .instructions(req.instructions())
                .cookTimeMinutes(req.cookTimeMinutes())
                .tags(req.tags())
                .source("user")
                .build();
        return RecipeResponse.from(recipeRepository.save(recipe));
    }

    @Transactional(readOnly = true)
    public PageResponse<RecipeSummaryResponse> listRecipes(Long userId, Pageable pageable) {
        Pageable capped = capPageable(pageable);
        Page<Recipe> page = recipeRepository.findByUserId(userId, capped);
        return PageResponse.from(page, RecipeSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public RecipeResponse getRecipe(Long userId, Long recipeId) {
        Recipe recipe = recipeRepository.findByIdAndUserIdWithIngredients(recipeId, userId)
                .orElseThrow(() -> new NotFoundException("Recipe not found"));
        return RecipeResponse.from(recipe);
    }

    @Transactional
    public RecipeResponse updateRecipe(Long userId, Long recipeId, UpdateRecipeRequest req) {
        Recipe recipe = loadOwnedRecipe(userId, recipeId);
        recipe.setTitle(req.title());
        recipe.setInstructions(req.instructions());
        recipe.setCookTimeMinutes(req.cookTimeMinutes());
        recipe.setTags(req.tags());
        return RecipeResponse.from(recipeRepository.save(recipe));
    }

    @Transactional
    public void deleteRecipe(Long userId, Long recipeId) {
        Recipe recipe = loadOwnedRecipe(userId, recipeId);
        recipeRepository.delete(recipe);
    }

    private Recipe loadOwnedRecipe(Long userId, Long recipeId) {
        return recipeRepository.findByIdAndUserId(recipeId, userId)
                .orElseThrow(() -> new NotFoundException("Recipe not found"));
    }

    private Pageable capPageable(Pageable pageable) {
        int size = pageable.getPageSize();
        if (size <= 0) {
            size = DEFAULT_PAGE_SIZE;
        } else if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "createdAt");
        return PageRequest.of(Math.max(pageable.getPageNumber(), 0), size, sort);
    }
}
