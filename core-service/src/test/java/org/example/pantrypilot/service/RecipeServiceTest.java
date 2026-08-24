package org.example.pantrypilot.service;

import java.util.List;
import java.util.Optional;

import org.example.pantrypilot.dto.CreateRecipeRequest;
import org.example.pantrypilot.dto.PageResponse;
import org.example.pantrypilot.dto.RecipeResponse;
import org.example.pantrypilot.dto.RecipeSummaryResponse;
import org.example.pantrypilot.dto.UpdateRecipeRequest;
import org.example.pantrypilot.model.Recipe;
import org.example.pantrypilot.model.RecipeIngredient;
import org.example.pantrypilot.model.User;
import org.example.pantrypilot.repository.RecipeRepository;
import org.example.pantrypilot.repository.UserRepository;
import org.example.pantrypilot.service.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    private static final Long USER_ID = 42L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long RECIPE_ID = 7L;

    @Mock private RecipeRepository recipeRepository;
    @Mock private UserRepository userRepository;

    private RecipeService service;

    @BeforeEach
    void setUp() {
        service = new RecipeService(recipeRepository, userRepository);
    }

    @Test
    void createRecipe_savesEntityWithOwnerAndSourceUser() {
        User owner = User.builder().id(USER_ID).build();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> {
            Recipe in = inv.getArgument(0);
            in.setId(RECIPE_ID);
            return in;
        });

        CreateRecipeRequest req = new CreateRecipeRequest(
                "Pancakes", "Mix and cook.", 15, new String[]{"breakfast"});

        RecipeResponse resp = service.createRecipe(USER_ID, req);

        assertThat(resp.id()).isEqualTo(RECIPE_ID);
        assertThat(resp.title()).isEqualTo("Pancakes");
        assertThat(resp.cookTimeMinutes()).isEqualTo(15);
        assertThat(resp.tags()).containsExactly("breakfast");
        assertThat(resp.ingredients()).isEmpty();
        verify(recipeRepository).save(argThat(r ->
                r.getUser() == owner
                        && "Pancakes".equals(r.getTitle())
                        && "Mix and cook.".equals(r.getInstructions())
                        && Integer.valueOf(15).equals(r.getCookTimeMinutes())
                        && "user".equals(r.getSource())));
    }

    @Test
    void listRecipes_returnsMappedPageWithMetadata() {
        Recipe a = recipe(1L, USER_ID, "A");
        Recipe b = recipe(2L, USER_ID, "B");
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Recipe> page = new PageImpl<>(List.of(a, b), pageable, 2);
        when(recipeRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(page);

        PageResponse<RecipeSummaryResponse> resp = service.listRecipes(USER_ID, pageable);

        assertThat(resp.content()).extracting(RecipeSummaryResponse::id).containsExactly(1L, 2L);
        assertThat(resp.totalElements()).isEqualTo(2);
        assertThat(resp.totalPages()).isEqualTo(1);
        assertThat(resp.page()).isEqualTo(0);
        assertThat(resp.size()).isEqualTo(20);
    }

    @Test
    void listRecipes_capsPageSizeAtMax() {
        Pageable requested = PageRequest.of(0, 500);
        when(recipeRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listRecipes(USER_ID, requested);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(recipeRepository).findByUserId(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(RecipeService.MAX_PAGE_SIZE);
    }

    @Test
    void listRecipes_appliesDefaultSortWhenUnsorted() {
        Pageable requested = PageRequest.of(2, 20);
        when(recipeRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listRecipes(USER_ID, requested);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(recipeRepository).findByUserId(eq(USER_ID), captor.capture());
        Sort sort = captor.getValue().getSort();
        assertThat(sort.getOrderFor("createdAt")).isNotNull();
        assertThat(sort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
    }

    @Test
    void getRecipe_ownedById_returnsResponseWithIngredients() {
        Recipe recipe = recipe(RECIPE_ID, USER_ID, "Soup");
        recipe.getIngredients().add(ingredient(11L, recipe, "Onion"));
        recipe.getIngredients().add(ingredient(12L, recipe, "Carrot"));
        when(recipeRepository.findByIdAndUserIdWithIngredients(RECIPE_ID, USER_ID))
                .thenReturn(Optional.of(recipe));

        RecipeResponse resp = service.getRecipe(USER_ID, RECIPE_ID);

        assertThat(resp.id()).isEqualTo(RECIPE_ID);
        assertThat(resp.ingredients()).extracting("id").containsExactly(11L, 12L);
    }

    @Test
    void getRecipe_notFoundOrOtherUser_throwsNotFound() {
        when(recipeRepository.findByIdAndUserIdWithIngredients(RECIPE_ID, OTHER_USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRecipe(OTHER_USER_ID, RECIPE_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateRecipe_success_updatesFieldsAndSaves() {
        Recipe recipe = recipe(RECIPE_ID, USER_ID, "Old");
        when(recipeRepository.findByIdAndUserId(RECIPE_ID, USER_ID)).thenReturn(Optional.of(recipe));
        when(recipeRepository.save(recipe)).thenReturn(recipe);

        UpdateRecipeRequest req = new UpdateRecipeRequest(
                "New Title", "New Instructions", 30, new String[]{"dinner"});

        RecipeResponse resp = service.updateRecipe(USER_ID, RECIPE_ID, req);

        assertThat(resp.title()).isEqualTo("New Title");
        assertThat(recipe.getTitle()).isEqualTo("New Title");
        assertThat(recipe.getInstructions()).isEqualTo("New Instructions");
        assertThat(recipe.getCookTimeMinutes()).isEqualTo(30);
        assertThat(recipe.getTags()).containsExactly("dinner");
    }

    @Test
    void updateRecipe_otherUsersRecipe_throwsNotFound() {
        when(recipeRepository.findByIdAndUserId(RECIPE_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        UpdateRecipeRequest req = new UpdateRecipeRequest("t", "i", null, null);

        assertThatThrownBy(() -> service.updateRecipe(OTHER_USER_ID, RECIPE_ID, req))
                .isInstanceOf(NotFoundException.class);

        verify(recipeRepository, never()).save(any());
    }

    @Test
    void deleteRecipe_owned_deletesEntity() {
        Recipe recipe = recipe(RECIPE_ID, USER_ID, "x");
        when(recipeRepository.findByIdAndUserId(RECIPE_ID, USER_ID)).thenReturn(Optional.of(recipe));

        service.deleteRecipe(USER_ID, RECIPE_ID);

        verify(recipeRepository).delete(recipe);
    }

    @Test
    void deleteRecipe_otherUsersRecipe_throwsNotFoundAndDoesNotDelete() {
        when(recipeRepository.findByIdAndUserId(RECIPE_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteRecipe(OTHER_USER_ID, RECIPE_ID))
                .isInstanceOf(NotFoundException.class);

        verify(recipeRepository, never()).delete(any(Recipe.class));
    }

    private static Recipe recipe(Long id, Long userId, String title) {
        return Recipe.builder()
                .id(id)
                .user(User.builder().id(userId).build())
                .title(title)
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
