package org.example.pantrypilot.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.example.pantrypilot.dto.CreateRecipeIngredientRequest;
import org.example.pantrypilot.dto.RecipeIngredientResponse;
import org.example.pantrypilot.dto.UpdateRecipeIngredientRequest;
import org.example.pantrypilot.model.Recipe;
import org.example.pantrypilot.model.RecipeIngredient;
import org.example.pantrypilot.model.User;
import org.example.pantrypilot.repository.RecipeIngredientRepository;
import org.example.pantrypilot.repository.RecipeRepository;
import org.example.pantrypilot.service.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeIngredientServiceTest {

    private static final Long USER_ID = 42L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long RECIPE_ID = 7L;
    private static final Long OTHER_RECIPE_ID = 8L;
    private static final Long INGREDIENT_ID = 11L;

    @Mock private RecipeRepository recipeRepository;
    @Mock private RecipeIngredientRepository recipeIngredientRepository;

    private RecipeIngredientService service;

    @BeforeEach
    void setUp() {
        service = new RecipeIngredientService(recipeRepository, recipeIngredientRepository);
    }

    @Test
    void addIngredient_owned_savesIngredientLinkedToRecipe() {
        Recipe recipe = recipe(RECIPE_ID, USER_ID);
        when(recipeRepository.findByIdAndUserId(RECIPE_ID, USER_ID)).thenReturn(Optional.of(recipe));
        when(recipeIngredientRepository.save(any(RecipeIngredient.class))).thenAnswer(inv -> {
            RecipeIngredient in = inv.getArgument(0);
            in.setId(INGREDIENT_ID);
            return in;
        });

        CreateRecipeIngredientRequest req = new CreateRecipeIngredientRequest(
                "Salt", new BigDecimal("2.5"), "tsp");

        RecipeIngredientResponse resp = service.addIngredient(USER_ID, RECIPE_ID, req);

        assertThat(resp.id()).isEqualTo(INGREDIENT_ID);
        assertThat(resp.name()).isEqualTo("Salt");
        verify(recipeIngredientRepository).save(argThat(i ->
                i.getRecipe() == recipe
                        && "Salt".equals(i.getName())
                        && new BigDecimal("2.5").compareTo(i.getQuantity()) == 0
                        && "tsp".equals(i.getUnit())));
    }

    @Test
    void addIngredient_recipeNotOwned_throwsNotFound() {
        when(recipeRepository.findByIdAndUserId(RECIPE_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        CreateRecipeIngredientRequest req = new CreateRecipeIngredientRequest("Salt", null, null);

        assertThatThrownBy(() -> service.addIngredient(OTHER_USER_ID, RECIPE_ID, req))
                .isInstanceOf(NotFoundException.class);

        verify(recipeIngredientRepository, never()).save(any());
    }

    @Test
    void updateIngredient_owned_updatesFieldsAndSaves() {
        Recipe recipe = recipe(RECIPE_ID, USER_ID);
        RecipeIngredient ingredient = ingredient(INGREDIENT_ID, recipe, "Old");
        when(recipeRepository.findByIdAndUserId(RECIPE_ID, USER_ID)).thenReturn(Optional.of(recipe));
        when(recipeIngredientRepository.findByIdAndRecipeId(INGREDIENT_ID, RECIPE_ID))
                .thenReturn(Optional.of(ingredient));
        when(recipeIngredientRepository.save(ingredient)).thenReturn(ingredient);

        UpdateRecipeIngredientRequest req = new UpdateRecipeIngredientRequest(
                "New", BigDecimal.ONE, "cup");

        RecipeIngredientResponse resp = service.updateIngredient(
                USER_ID, RECIPE_ID, INGREDIENT_ID, req);

        assertThat(resp.name()).isEqualTo("New");
        assertThat(ingredient.getName()).isEqualTo("New");
        assertThat(ingredient.getUnit()).isEqualTo("cup");
        assertThat(ingredient.getQuantity()).isEqualByComparingTo("1");
    }

    @Test
    void updateIngredient_recipeNotOwned_throwsNotFoundBeforeLookingUpIngredient() {
        when(recipeRepository.findByIdAndUserId(RECIPE_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        UpdateRecipeIngredientRequest req = new UpdateRecipeIngredientRequest("n", null, null);

        assertThatThrownBy(() -> service.updateIngredient(
                OTHER_USER_ID, RECIPE_ID, INGREDIENT_ID, req))
                .isInstanceOf(NotFoundException.class);

        verify(recipeIngredientRepository, never()).findByIdAndRecipeId(any(), any());
        verify(recipeIngredientRepository, never()).save(any());
    }

    @Test
    void updateIngredient_ingredientBelongsToDifferentRecipe_throwsNotFound() {
        Recipe recipe = recipe(RECIPE_ID, USER_ID);
        when(recipeRepository.findByIdAndUserId(RECIPE_ID, USER_ID)).thenReturn(Optional.of(recipe));
        when(recipeIngredientRepository.findByIdAndRecipeId(INGREDIENT_ID, RECIPE_ID))
                .thenReturn(Optional.empty());

        UpdateRecipeIngredientRequest req = new UpdateRecipeIngredientRequest("n", null, null);

        assertThatThrownBy(() -> service.updateIngredient(
                USER_ID, RECIPE_ID, INGREDIENT_ID, req))
                .isInstanceOf(NotFoundException.class);

        verify(recipeIngredientRepository, never()).save(any());
    }

    @Test
    void deleteIngredient_owned_deletesEntity() {
        Recipe recipe = recipe(RECIPE_ID, USER_ID);
        RecipeIngredient ingredient = ingredient(INGREDIENT_ID, recipe, "x");
        when(recipeRepository.findByIdAndUserId(RECIPE_ID, USER_ID)).thenReturn(Optional.of(recipe));
        when(recipeIngredientRepository.findByIdAndRecipeId(INGREDIENT_ID, RECIPE_ID))
                .thenReturn(Optional.of(ingredient));

        service.deleteIngredient(USER_ID, RECIPE_ID, INGREDIENT_ID);

        verify(recipeIngredientRepository).delete(ingredient);
    }

    @Test
    void deleteIngredient_ingredientFromDifferentRecipe_throwsNotFoundAndDoesNotDelete() {
        Recipe recipe = recipe(RECIPE_ID, USER_ID);
        when(recipeRepository.findByIdAndUserId(RECIPE_ID, USER_ID)).thenReturn(Optional.of(recipe));
        when(recipeIngredientRepository.findByIdAndRecipeId(INGREDIENT_ID, RECIPE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteIngredient(USER_ID, RECIPE_ID, INGREDIENT_ID))
                .isInstanceOf(NotFoundException.class);

        verify(recipeIngredientRepository, never()).delete(any(RecipeIngredient.class));
    }

    @Test
    void deleteIngredient_recipeNotOwned_throwsNotFound() {
        when(recipeRepository.findByIdAndUserId(OTHER_RECIPE_ID, OTHER_USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteIngredient(
                OTHER_USER_ID, OTHER_RECIPE_ID, INGREDIENT_ID))
                .isInstanceOf(NotFoundException.class);

        verify(recipeIngredientRepository, never()).findByIdAndRecipeId(any(), any());
        verify(recipeIngredientRepository, never()).delete(any(RecipeIngredient.class));
    }

    private static Recipe recipe(Long id, Long userId) {
        return Recipe.builder()
                .id(id)
                .user(User.builder().id(userId).build())
                .title("r" + id)
                .instructions("instr")
                .build();
    }

    private static RecipeIngredient ingredient(Long id, Recipe recipe, String name) {
        return RecipeIngredient.builder()
                .id(id)
                .recipe(recipe)
                .name(name)
                .build();
    }
}
