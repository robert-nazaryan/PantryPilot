package org.example.pantrypilot.service;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.dto.CreateShoppingListItemRequest;
import org.example.pantrypilot.dto.ShoppingListItemResponse;
import org.example.pantrypilot.dto.ToggleShoppingListItemCheckedRequest;
import org.example.pantrypilot.dto.UpdateShoppingListItemRequest;
import org.example.pantrypilot.model.ShoppingList;
import org.example.pantrypilot.model.ShoppingListItem;
import org.example.pantrypilot.repository.ShoppingListItemRepository;
import org.example.pantrypilot.repository.ShoppingListRepository;
import org.example.pantrypilot.service.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShoppingListItemService {

    private final ShoppingListRepository shoppingListRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;

    @Transactional
    public ShoppingListItemResponse addItem(
            Long userId, Long listId, CreateShoppingListItemRequest req) {
        ShoppingList list = loadOwnedList(userId, listId);
        ShoppingListItem item = ShoppingListItem.builder()
                .shoppingList(list)
                .name(req.name())
                .quantity(req.quantity())
                .unit(req.unit())
                .build();
        return ShoppingListItemResponse.from(shoppingListItemRepository.save(item));
    }

    @Transactional
    public ShoppingListItemResponse updateItem(
            Long userId, Long listId, Long itemId, UpdateShoppingListItemRequest req) {
        ShoppingListItem item = loadOwnedItem(userId, listId, itemId);
        item.setName(req.name());
        item.setQuantity(req.quantity());
        item.setUnit(req.unit());
        item.setChecked(req.checked());
        return ShoppingListItemResponse.from(shoppingListItemRepository.save(item));
    }

    @Transactional
    public ShoppingListItemResponse setChecked(
            Long userId, Long listId, Long itemId, ToggleShoppingListItemCheckedRequest req) {
        ShoppingListItem item = loadOwnedItem(userId, listId, itemId);
        item.setChecked(req.checked());
        return ShoppingListItemResponse.from(shoppingListItemRepository.save(item));
    }

    @Transactional
    public void deleteItem(Long userId, Long listId, Long itemId) {
        ShoppingListItem item = loadOwnedItem(userId, listId, itemId);
        shoppingListItemRepository.delete(item);
    }

    private ShoppingList loadOwnedList(Long userId, Long listId) {
        return shoppingListRepository.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new NotFoundException("Shopping list not found"));
    }

    private ShoppingListItem loadOwnedItem(Long userId, Long listId, Long itemId) {
        loadOwnedList(userId, listId);
        return shoppingListItemRepository.findByIdAndShoppingListId(itemId, listId)
                .orElseThrow(() -> new NotFoundException("Shopping list item not found"));
    }
}
