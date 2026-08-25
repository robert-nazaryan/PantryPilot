package org.example.pantrypilot.service;

import lombok.RequiredArgsConstructor;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShoppingListService {

    static final int MAX_PAGE_SIZE = 100;
    static final int DEFAULT_PAGE_SIZE = 20;
    static final String DEFAULT_LIST_NAME = "Shopping List";

    private final ShoppingListRepository shoppingListRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    @Transactional
    public ShoppingListResponse createList(Long userId, CreateShoppingListRequest req) {
        User owner = userRepository.getReferenceById(userId);
        ShoppingList list = ShoppingList.builder()
                .user(owner)
                .name(resolveName(req.name()))
                .build();
        return ShoppingListResponse.from(shoppingListRepository.save(list));
    }

    @Transactional(readOnly = true)
    public PageResponse<ShoppingListSummaryResponse> listLists(Long userId, Pageable pageable) {
        Pageable capped = capPageable(pageable);
        Page<ShoppingList> page = shoppingListRepository.findByUserId(userId, capped);
        return PageResponse.from(page, ShoppingListSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public ShoppingListResponse getList(Long userId, Long listId) {
        ShoppingList list = shoppingListRepository.findByIdAndUserIdWithItems(listId, userId)
                .orElseThrow(() -> new NotFoundException("Shopping list not found"));
        return ShoppingListResponse.from(list);
    }

    @Transactional
    public ShoppingListResponse updateList(Long userId, Long listId, UpdateShoppingListRequest req) {
        ShoppingList list = loadOwnedList(userId, listId);
        list.setName(req.name());
        list.setActive(req.active());
        return ShoppingListResponse.from(shoppingListRepository.save(list));
    }

    @Transactional
    public void deleteList(Long userId, Long listId) {
        ShoppingList list = loadOwnedList(userId, listId);
        shoppingListRepository.delete(list);
    }

    @Transactional
    public ShoppingListResponse generateFromRecipe(Long userId, Long recipeId) {
        Recipe recipe = recipeRepository.findByIdAndUserIdWithIngredients(recipeId, userId)
                .orElseThrow(() -> new NotFoundException("Recipe not found"));
        User owner = userRepository.getReferenceById(userId);
        ShoppingList list = ShoppingList.builder()
                .user(owner)
                .name(recipe.getTitle())
                .build();
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            list.getItems().add(itemFromIngredient(list, ingredient));
        }
        return ShoppingListResponse.from(shoppingListRepository.save(list));
    }

    private ShoppingList loadOwnedList(Long userId, Long listId) {
        return shoppingListRepository.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new NotFoundException("Shopping list not found"));
    }

    private static ShoppingListItem itemFromIngredient(ShoppingList list, RecipeIngredient ingredient) {
        return ShoppingListItem.builder()
                .shoppingList(list)
                .name(ingredient.getName())
                .quantity(ingredient.getQuantity())
                .unit(ingredient.getUnit())
                .build();
    }

    private static String resolveName(String name) {
        if (name == null || name.isBlank()) {
            return DEFAULT_LIST_NAME;
        }
        return name;
    }

    private static Pageable capPageable(Pageable pageable) {
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
