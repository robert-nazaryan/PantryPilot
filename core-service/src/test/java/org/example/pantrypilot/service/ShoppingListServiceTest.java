package org.example.pantrypilot.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.example.pantrypilot.dto.CreateShoppingListRequest;
import org.example.pantrypilot.dto.PageResponse;
import org.example.pantrypilot.dto.ShoppingListResponse;
import org.example.pantrypilot.dto.ShoppingListSummaryResponse;
import org.example.pantrypilot.dto.UpdateShoppingListRequest;
import org.example.pantrypilot.model.Recipe;
import org.example.pantrypilot.model.RecipeIngredient;
import org.example.pantrypilot.model.ShoppingList;
import org.example.pantrypilot.model.ShoppingListItem;
import org.example.pantrypilot.model.User;
import org.example.pantrypilot.repository.RecipeRepository;
import org.example.pantrypilot.repository.ShoppingListRepository;
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
class ShoppingListServiceTest {

    private static final Long USER_ID = 42L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long LIST_ID = 7L;
    private static final Long RECIPE_ID = 33L;

    @Mock private ShoppingListRepository shoppingListRepository;
    @Mock private RecipeRepository recipeRepository;
    @Mock private UserRepository userRepository;

    private ShoppingListService service;

    @BeforeEach
    void setUp() {
        service = new ShoppingListService(
                shoppingListRepository, recipeRepository, userRepository);
    }

    @Test
    void createList_withGivenName_savesListForOwner() {
        User owner = User.builder().id(USER_ID).build();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
        when(shoppingListRepository.save(any(ShoppingList.class))).thenAnswer(inv -> {
            ShoppingList in = inv.getArgument(0);
            in.setId(LIST_ID);
            return in;
        });

        ShoppingListResponse resp = service.createList(
                USER_ID, new CreateShoppingListRequest("Weekend"));

        assertThat(resp.id()).isEqualTo(LIST_ID);
        assertThat(resp.name()).isEqualTo("Weekend");
        assertThat(resp.active()).isFalse();
        assertThat(resp.items()).isEmpty();
        verify(shoppingListRepository).save(argThat(l ->
                l.getUser() == owner && "Weekend".equals(l.getName()) && !l.isActive()));
    }

    @Test
    void createList_nullName_defaultsToShoppingList() {
        User owner = User.builder().id(USER_ID).build();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
        when(shoppingListRepository.save(any(ShoppingList.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createList(USER_ID, new CreateShoppingListRequest(null));

        verify(shoppingListRepository).save(argThat(l ->
                ShoppingListService.DEFAULT_LIST_NAME.equals(l.getName())));
    }

    @Test
    void createList_blankName_defaultsToShoppingList() {
        User owner = User.builder().id(USER_ID).build();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
        when(shoppingListRepository.save(any(ShoppingList.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createList(USER_ID, new CreateShoppingListRequest("   "));

        verify(shoppingListRepository).save(argThat(l ->
                ShoppingListService.DEFAULT_LIST_NAME.equals(l.getName())));
    }

    @Test
    void listLists_returnsMappedPageWithMetadata() {
        ShoppingList a = list(1L, USER_ID, "A");
        ShoppingList b = list(2L, USER_ID, "B");
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ShoppingList> page = new PageImpl<>(List.of(a, b), pageable, 2);
        when(shoppingListRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(page);

        PageResponse<ShoppingListSummaryResponse> resp = service.listLists(USER_ID, pageable);

        assertThat(resp.content()).extracting(ShoppingListSummaryResponse::id).containsExactly(1L, 2L);
        assertThat(resp.totalElements()).isEqualTo(2);
        assertThat(resp.totalPages()).isEqualTo(1);
        assertThat(resp.page()).isEqualTo(0);
        assertThat(resp.size()).isEqualTo(20);
    }

    @Test
    void listLists_capsPageSizeAtMax() {
        Pageable requested = PageRequest.of(0, 500);
        when(shoppingListRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listLists(USER_ID, requested);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(shoppingListRepository).findByUserId(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(ShoppingListService.MAX_PAGE_SIZE);
    }

    @Test
    void listLists_appliesDefaultSortWhenUnsorted() {
        Pageable requested = PageRequest.of(2, 20);
        when(shoppingListRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listLists(USER_ID, requested);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(shoppingListRepository).findByUserId(eq(USER_ID), captor.capture());
        Sort sort = captor.getValue().getSort();
        assertThat(sort.getOrderFor("createdAt")).isNotNull();
        assertThat(sort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getList_ownedById_returnsResponseWithItems() {
        ShoppingList l = list(LIST_ID, USER_ID, "Groceries");
        l.getItems().add(item(11L, l, "Bread"));
        l.getItems().add(item(12L, l, "Butter"));
        when(shoppingListRepository.findByIdAndUserIdWithItems(LIST_ID, USER_ID))
                .thenReturn(Optional.of(l));

        ShoppingListResponse resp = service.getList(USER_ID, LIST_ID);

        assertThat(resp.id()).isEqualTo(LIST_ID);
        assertThat(resp.items()).extracting("id").containsExactly(11L, 12L);
    }

    @Test
    void getList_notFoundOrOtherUser_throwsNotFound() {
        when(shoppingListRepository.findByIdAndUserIdWithItems(LIST_ID, OTHER_USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getList(OTHER_USER_ID, LIST_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateList_success_updatesFieldsAndSaves() {
        ShoppingList l = list(LIST_ID, USER_ID, "Old");
        when(shoppingListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(l));
        when(shoppingListRepository.save(l)).thenReturn(l);

        UpdateShoppingListRequest req = new UpdateShoppingListRequest("Renamed", true);

        ShoppingListResponse resp = service.updateList(USER_ID, LIST_ID, req);

        assertThat(resp.name()).isEqualTo("Renamed");
        assertThat(resp.active()).isTrue();
        assertThat(l.getName()).isEqualTo("Renamed");
        assertThat(l.isActive()).isTrue();
    }

    @Test
    void updateList_otherUsersList_throwsNotFoundAndDoesNotSave() {
        when(shoppingListRepository.findByIdAndUserId(LIST_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        UpdateShoppingListRequest req = new UpdateShoppingListRequest("x", false);

        assertThatThrownBy(() -> service.updateList(OTHER_USER_ID, LIST_ID, req))
                .isInstanceOf(NotFoundException.class);

        verify(shoppingListRepository, never()).save(any());
    }

    @Test
    void deleteList_owned_deletesEntity() {
        ShoppingList l = list(LIST_ID, USER_ID, "x");
        when(shoppingListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(l));

        service.deleteList(USER_ID, LIST_ID);

        verify(shoppingListRepository).delete(l);
    }

    @Test
    void deleteList_otherUsersList_throwsNotFoundAndDoesNotDelete() {
        when(shoppingListRepository.findByIdAndUserId(LIST_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteList(OTHER_USER_ID, LIST_ID))
                .isInstanceOf(NotFoundException.class);

        verify(shoppingListRepository, never()).delete(any(ShoppingList.class));
    }

    @Test
    void generateFromRecipe_withIngredients_createsListWithCopiedItems() {
        Recipe recipe = recipe(RECIPE_ID, USER_ID, "Pancakes");
        recipe.getIngredients().add(ingredient(101L, recipe, "Flour", new BigDecimal("200"), "g"));
        recipe.getIngredients().add(ingredient(102L, recipe, "Milk", new BigDecimal("250"), "ml"));
        User owner = User.builder().id(USER_ID).build();
        when(recipeRepository.findByIdAndUserIdWithIngredients(RECIPE_ID, USER_ID))
                .thenReturn(Optional.of(recipe));
        when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
        when(shoppingListRepository.save(any(ShoppingList.class))).thenAnswer(inv -> {
            ShoppingList in = inv.getArgument(0);
            in.setId(LIST_ID);
            in.getItems().forEach(it -> it.setId(500L + in.getItems().indexOf(it)));
            return in;
        });

        ShoppingListResponse resp = service.generateFromRecipe(USER_ID, RECIPE_ID);

        assertThat(resp.id()).isEqualTo(LIST_ID);
        assertThat(resp.name()).isEqualTo("Pancakes");
        assertThat(resp.items()).extracting("name").containsExactly("Flour", "Milk");
        assertThat(resp.items()).extracting("unit").containsExactly("g", "ml");
        verify(shoppingListRepository).save(argThat(l ->
                l.getUser() == owner
                        && "Pancakes".equals(l.getName())
                        && l.getItems().size() == 2
                        && l.getItems().stream().allMatch(it -> it.getShoppingList() == l)));
    }

    @Test
    void generateFromRecipe_withZeroIngredients_createsEmptyList() {
        Recipe recipe = recipe(RECIPE_ID, USER_ID, "Empty Recipe");
        User owner = User.builder().id(USER_ID).build();
        when(recipeRepository.findByIdAndUserIdWithIngredients(RECIPE_ID, USER_ID))
                .thenReturn(Optional.of(recipe));
        when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
        when(shoppingListRepository.save(any(ShoppingList.class))).thenAnswer(inv -> {
            ShoppingList in = inv.getArgument(0);
            in.setId(LIST_ID);
            return in;
        });

        ShoppingListResponse resp = service.generateFromRecipe(USER_ID, RECIPE_ID);

        assertThat(resp.id()).isEqualTo(LIST_ID);
        assertThat(resp.items()).isEmpty();
    }

    @Test
    void generateFromRecipe_otherUsersRecipe_throwsNotFoundAndSavesNothing() {
        when(recipeRepository.findByIdAndUserIdWithIngredients(RECIPE_ID, OTHER_USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateFromRecipe(OTHER_USER_ID, RECIPE_ID))
                .isInstanceOf(NotFoundException.class);

        verify(shoppingListRepository, never()).save(any());
    }

    private static ShoppingList list(Long id, Long userId, String name) {
        return ShoppingList.builder()
                .id(id)
                .user(User.builder().id(userId).build())
                .name(name)
                .build();
    }

    private static Recipe recipe(Long id, Long userId, String title) {
        return Recipe.builder()
                .id(id)
                .user(User.builder().id(userId).build())
                .title(title)
                .instructions("instr")
                .build();
    }

    private static RecipeIngredient ingredient(
            Long id, Recipe recipe, String name, BigDecimal qty, String unit) {
        return RecipeIngredient.builder()
                .id(id)
                .recipe(recipe)
                .name(name)
                .quantity(qty)
                .unit(unit)
                .build();
    }

    private static ShoppingListItem item(Long id, ShoppingList list, String name) {
        return ShoppingListItem.builder()
                .id(id)
                .shoppingList(list)
                .name(name)
                .build();
    }
}
