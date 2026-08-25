package org.example.pantrypilot.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.example.pantrypilot.dto.CreateShoppingListItemRequest;
import org.example.pantrypilot.dto.ShoppingListItemResponse;
import org.example.pantrypilot.dto.ToggleShoppingListItemCheckedRequest;
import org.example.pantrypilot.dto.UpdateShoppingListItemRequest;
import org.example.pantrypilot.model.ShoppingList;
import org.example.pantrypilot.model.ShoppingListItem;
import org.example.pantrypilot.model.User;
import org.example.pantrypilot.repository.ShoppingListItemRepository;
import org.example.pantrypilot.repository.ShoppingListRepository;
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
class ShoppingListItemServiceTest {

    private static final Long USER_ID = 42L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long LIST_ID = 7L;
    private static final Long OTHER_LIST_ID = 8L;
    private static final Long ITEM_ID = 11L;

    @Mock private ShoppingListRepository shoppingListRepository;
    @Mock private ShoppingListItemRepository shoppingListItemRepository;

    private ShoppingListItemService service;

    @BeforeEach
    void setUp() {
        service = new ShoppingListItemService(shoppingListRepository, shoppingListItemRepository);
    }

    @Test
    void addItem_owned_savesItemLinkedToList() {
        ShoppingList list = list(LIST_ID, USER_ID);
        when(shoppingListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
        when(shoppingListItemRepository.save(any(ShoppingListItem.class))).thenAnswer(inv -> {
            ShoppingListItem in = inv.getArgument(0);
            in.setId(ITEM_ID);
            return in;
        });

        CreateShoppingListItemRequest req = new CreateShoppingListItemRequest(
                "Bread", BigDecimal.valueOf(2), "loaves");

        ShoppingListItemResponse resp = service.addItem(USER_ID, LIST_ID, req);

        assertThat(resp.id()).isEqualTo(ITEM_ID);
        assertThat(resp.name()).isEqualTo("Bread");
        assertThat(resp.checked()).isFalse();
        verify(shoppingListItemRepository).save(argThat(i ->
                i.getShoppingList() == list
                        && "Bread".equals(i.getName())
                        && BigDecimal.valueOf(2).compareTo(i.getQuantity()) == 0
                        && "loaves".equals(i.getUnit())
                        && !i.isChecked()));
    }

    @Test
    void addItem_listNotOwned_throwsNotFound() {
        when(shoppingListRepository.findByIdAndUserId(LIST_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        CreateShoppingListItemRequest req = new CreateShoppingListItemRequest("Bread", null, null);

        assertThatThrownBy(() -> service.addItem(OTHER_USER_ID, LIST_ID, req))
                .isInstanceOf(NotFoundException.class);

        verify(shoppingListItemRepository, never()).save(any());
    }

    @Test
    void updateItem_owned_updatesAllFieldsIncludingChecked() {
        ShoppingList list = list(LIST_ID, USER_ID);
        ShoppingListItem item = item(ITEM_ID, list, "Old", false);
        when(shoppingListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
        when(shoppingListItemRepository.findByIdAndShoppingListId(ITEM_ID, LIST_ID))
                .thenReturn(Optional.of(item));
        when(shoppingListItemRepository.save(item)).thenReturn(item);

        UpdateShoppingListItemRequest req = new UpdateShoppingListItemRequest(
                "New", BigDecimal.ONE, "kg", true);

        ShoppingListItemResponse resp = service.updateItem(USER_ID, LIST_ID, ITEM_ID, req);

        assertThat(resp.name()).isEqualTo("New");
        assertThat(resp.checked()).isTrue();
        assertThat(item.getName()).isEqualTo("New");
        assertThat(item.getUnit()).isEqualTo("kg");
        assertThat(item.getQuantity()).isEqualByComparingTo("1");
        assertThat(item.isChecked()).isTrue();
    }

    @Test
    void updateItem_listNotOwned_throwsNotFoundBeforeLookingUpItem() {
        when(shoppingListRepository.findByIdAndUserId(LIST_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        UpdateShoppingListItemRequest req = new UpdateShoppingListItemRequest("n", null, null, false);

        assertThatThrownBy(() -> service.updateItem(OTHER_USER_ID, LIST_ID, ITEM_ID, req))
                .isInstanceOf(NotFoundException.class);

        verify(shoppingListItemRepository, never()).findByIdAndShoppingListId(any(), any());
        verify(shoppingListItemRepository, never()).save(any());
    }

    @Test
    void updateItem_itemBelongsToDifferentList_throwsNotFound() {
        ShoppingList list = list(LIST_ID, USER_ID);
        when(shoppingListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
        when(shoppingListItemRepository.findByIdAndShoppingListId(ITEM_ID, LIST_ID))
                .thenReturn(Optional.empty());

        UpdateShoppingListItemRequest req = new UpdateShoppingListItemRequest("n", null, null, false);

        assertThatThrownBy(() -> service.updateItem(USER_ID, LIST_ID, ITEM_ID, req))
                .isInstanceOf(NotFoundException.class);

        verify(shoppingListItemRepository, never()).save(any());
    }

    @Test
    void setChecked_toggle_setsFlagAndSaves() {
        ShoppingList list = list(LIST_ID, USER_ID);
        ShoppingListItem item = item(ITEM_ID, list, "x", false);
        when(shoppingListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
        when(shoppingListItemRepository.findByIdAndShoppingListId(ITEM_ID, LIST_ID))
                .thenReturn(Optional.of(item));
        when(shoppingListItemRepository.save(item)).thenReturn(item);

        ShoppingListItemResponse resp = service.setChecked(
                USER_ID, LIST_ID, ITEM_ID, new ToggleShoppingListItemCheckedRequest(true));

        assertThat(resp.checked()).isTrue();
        assertThat(item.isChecked()).isTrue();
    }

    @Test
    void setChecked_untick_setsFlagFalseAndSaves() {
        ShoppingList list = list(LIST_ID, USER_ID);
        ShoppingListItem item = item(ITEM_ID, list, "x", true);
        when(shoppingListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
        when(shoppingListItemRepository.findByIdAndShoppingListId(ITEM_ID, LIST_ID))
                .thenReturn(Optional.of(item));
        when(shoppingListItemRepository.save(item)).thenReturn(item);

        service.setChecked(USER_ID, LIST_ID, ITEM_ID,
                new ToggleShoppingListItemCheckedRequest(false));

        assertThat(item.isChecked()).isFalse();
    }

    @Test
    void setChecked_itemBelongsToDifferentList_throwsNotFound() {
        ShoppingList list = list(LIST_ID, USER_ID);
        when(shoppingListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
        when(shoppingListItemRepository.findByIdAndShoppingListId(ITEM_ID, LIST_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setChecked(USER_ID, LIST_ID, ITEM_ID,
                new ToggleShoppingListItemCheckedRequest(true)))
                .isInstanceOf(NotFoundException.class);

        verify(shoppingListItemRepository, never()).save(any());
    }

    @Test
    void deleteItem_owned_deletesEntity() {
        ShoppingList list = list(LIST_ID, USER_ID);
        ShoppingListItem item = item(ITEM_ID, list, "x", false);
        when(shoppingListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
        when(shoppingListItemRepository.findByIdAndShoppingListId(ITEM_ID, LIST_ID))
                .thenReturn(Optional.of(item));

        service.deleteItem(USER_ID, LIST_ID, ITEM_ID);

        verify(shoppingListItemRepository).delete(item);
    }

    @Test
    void deleteItem_itemFromDifferentList_throwsNotFoundAndDoesNotDelete() {
        ShoppingList list = list(LIST_ID, USER_ID);
        when(shoppingListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
        when(shoppingListItemRepository.findByIdAndShoppingListId(ITEM_ID, LIST_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteItem(USER_ID, LIST_ID, ITEM_ID))
                .isInstanceOf(NotFoundException.class);

        verify(shoppingListItemRepository, never()).delete(any(ShoppingListItem.class));
    }

    @Test
    void deleteItem_listNotOwned_throwsNotFound() {
        when(shoppingListRepository.findByIdAndUserId(OTHER_LIST_ID, OTHER_USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteItem(OTHER_USER_ID, OTHER_LIST_ID, ITEM_ID))
                .isInstanceOf(NotFoundException.class);

        verify(shoppingListItemRepository, never()).findByIdAndShoppingListId(any(), any());
        verify(shoppingListItemRepository, never()).delete(any(ShoppingListItem.class));
    }

    private static ShoppingList list(Long id, Long userId) {
        return ShoppingList.builder()
                .id(id)
                .user(User.builder().id(userId).build())
                .name("list" + id)
                .build();
    }

    private static ShoppingListItem item(Long id, ShoppingList list, String name, boolean checked) {
        return ShoppingListItem.builder()
                .id(id)
                .shoppingList(list)
                .name(name)
                .checked(checked)
                .build();
    }
}
