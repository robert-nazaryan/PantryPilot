package org.example.pantrypilot.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.example.pantrypilot.dto.ConsumeQuantityRequest;
import org.example.pantrypilot.dto.CreatePantryItemRequest;
import org.example.pantrypilot.dto.PantryItemResponse;
import org.example.pantrypilot.dto.UpdatePantryItemRequest;
import org.example.pantrypilot.model.PantryItem;
import org.example.pantrypilot.model.User;
import org.example.pantrypilot.repository.PantryItemRepository;
import org.example.pantrypilot.repository.UserRepository;
import org.example.pantrypilot.service.exception.InsufficientQuantityException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PantryItemServiceTest {

    private static final Long USER_ID = 42L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long ITEM_ID = 7L;

    @Mock private PantryItemRepository pantryItemRepository;
    @Mock private UserRepository userRepository;

    private PantryItemService service;

    @BeforeEach
    void setUp() {
        service = new PantryItemService(pantryItemRepository, userRepository);
    }

    @Test
    void createItem_savesEntityWithOwnerAndCategorySourceUser() {
        User owner = User.builder().id(USER_ID).build();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
        when(pantryItemRepository.save(any(PantryItem.class))).thenAnswer(inv -> {
            PantryItem in = inv.getArgument(0);
            in.setId(ITEM_ID);
            return in;
        });

        CreatePantryItemRequest req = new CreatePantryItemRequest(
                "Milk", new BigDecimal("2.000"), "L", "dairy", LocalDate.now().plusDays(5));

        PantryItemResponse resp = service.createItem(USER_ID, req);

        assertThat(resp.id()).isEqualTo(ITEM_ID);
        assertThat(resp.name()).isEqualTo("Milk");
        assertThat(resp.category()).isEqualTo("dairy");
        verify(pantryItemRepository).save(argThat(item ->
                item.getUser() == owner
                        && "Milk".equals(item.getName())
                        && new BigDecimal("2.000").compareTo(item.getQuantity()) == 0
                        && "L".equals(item.getUnit())
                        && "dairy".equals(item.getCategory())
                        && "user".equals(item.getCategorySource())));
    }

    @Test
    void createItem_nullCategory_leavesCategorySourceNull() {
        User owner = User.builder().id(USER_ID).build();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
        when(pantryItemRepository.save(any(PantryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createItem(USER_ID, new CreatePantryItemRequest(
                "Salt", new BigDecimal("1.0"), "kg", null, null));

        verify(pantryItemRepository).save(argThat(item ->
                item.getCategory() == null && item.getCategorySource() == null));
    }

    @Test
    void listItems_returnsMappedResponsesInRepositoryOrder() {
        PantryItem a = pantryItem(1L, USER_ID, "Bread", new BigDecimal("1"), LocalDate.now());
        PantryItem b = pantryItem(2L, USER_ID, "Rice", new BigDecimal("3"), null);
        when(pantryItemRepository.findByUserIdOrderByExpiryDateAscNullsLast(USER_ID))
                .thenReturn(List.of(a, b));

        List<PantryItemResponse> resp = service.listItems(USER_ID);

        assertThat(resp).extracting(PantryItemResponse::id).containsExactly(1L, 2L);
    }

    @Test
    void getItem_ownedById_returnsResponse() {
        PantryItem item = pantryItem(ITEM_ID, USER_ID, "Cheese", new BigDecimal("0.500"), null);
        when(pantryItemRepository.findByIdAndUserId(ITEM_ID, USER_ID)).thenReturn(Optional.of(item));

        PantryItemResponse resp = service.getItem(USER_ID, ITEM_ID);

        assertThat(resp.id()).isEqualTo(ITEM_ID);
        assertThat(resp.name()).isEqualTo("Cheese");
    }

    @Test
    void getItem_notFoundOrOtherUser_throwsNotFound() {
        when(pantryItemRepository.findByIdAndUserId(ITEM_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getItem(OTHER_USER_ID, ITEM_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateItem_success_updatesAllFieldsAndSaves() {
        PantryItem item = pantryItem(ITEM_ID, USER_ID, "Old", new BigDecimal("1"), null);
        when(pantryItemRepository.findByIdAndUserId(ITEM_ID, USER_ID)).thenReturn(Optional.of(item));
        when(pantryItemRepository.save(item)).thenReturn(item);

        UpdatePantryItemRequest req = new UpdatePantryItemRequest(
                "New Name", new BigDecimal("5.5"), "kg", "grains", LocalDate.now().plusDays(1));

        PantryItemResponse resp = service.updateItem(USER_ID, ITEM_ID, req);

        assertThat(resp.name()).isEqualTo("New Name");
        assertThat(item.getName()).isEqualTo("New Name");
        assertThat(item.getQuantity()).isEqualByComparingTo("5.5");
        assertThat(item.getUnit()).isEqualTo("kg");
        assertThat(item.getCategory()).isEqualTo("grains");
        assertThat(item.getCategorySource()).isEqualTo("user");
    }

    @Test
    void updateItem_otherUsersItem_throwsNotFound() {
        when(pantryItemRepository.findByIdAndUserId(ITEM_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        UpdatePantryItemRequest req = new UpdatePantryItemRequest(
                "x", new BigDecimal("1"), "u", null, null);

        assertThatThrownBy(() -> service.updateItem(OTHER_USER_ID, ITEM_ID, req))
                .isInstanceOf(NotFoundException.class);

        verify(pantryItemRepository, never()).save(any());
    }

    @Test
    void consumeQuantity_partial_subtractsAndSaves() {
        PantryItem item = pantryItem(ITEM_ID, USER_ID, "Milk", new BigDecimal("2.000"), null);
        when(pantryItemRepository.findByIdAndUserId(ITEM_ID, USER_ID)).thenReturn(Optional.of(item));
        when(pantryItemRepository.save(item)).thenReturn(item);

        PantryItemResponse resp = service.consumeQuantity(
                USER_ID, ITEM_ID, new ConsumeQuantityRequest(new BigDecimal("0.500")));

        assertThat(item.getQuantity()).isEqualByComparingTo("1.500");
        assertThat(resp.quantity()).isEqualByComparingTo("1.500");
    }

    @Test
    void consumeQuantity_exactlyToZero_savesRowWithZeroDoesNotDelete() {
        PantryItem item = pantryItem(ITEM_ID, USER_ID, "Bread", new BigDecimal("1"), null);
        when(pantryItemRepository.findByIdAndUserId(ITEM_ID, USER_ID)).thenReturn(Optional.of(item));
        when(pantryItemRepository.save(item)).thenReturn(item);

        service.consumeQuantity(USER_ID, ITEM_ID, new ConsumeQuantityRequest(new BigDecimal("1")));

        assertThat(item.getQuantity()).isEqualByComparingTo("0");
        verify(pantryItemRepository).save(item);
        verify(pantryItemRepository, never()).delete(any());
    }

    @Test
    void consumeQuantity_moreThanAvailable_throwsInsufficient() {
        PantryItem item = pantryItem(ITEM_ID, USER_ID, "Rice", new BigDecimal("1"), null);
        when(pantryItemRepository.findByIdAndUserId(ITEM_ID, USER_ID)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.consumeQuantity(
                USER_ID, ITEM_ID, new ConsumeQuantityRequest(new BigDecimal("2"))))
                .isInstanceOf(InsufficientQuantityException.class);

        assertThat(item.getQuantity()).isEqualByComparingTo("1");
        verify(pantryItemRepository, never()).save(any());
    }

    @Test
    void consumeQuantity_otherUsersItem_throwsNotFound() {
        when(pantryItemRepository.findByIdAndUserId(ITEM_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consumeQuantity(
                OTHER_USER_ID, ITEM_ID, new ConsumeQuantityRequest(new BigDecimal("1"))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteItem_owned_deletesEntity() {
        PantryItem item = pantryItem(ITEM_ID, USER_ID, "x", new BigDecimal("1"), null);
        when(pantryItemRepository.findByIdAndUserId(ITEM_ID, USER_ID)).thenReturn(Optional.of(item));

        service.deleteItem(USER_ID, ITEM_ID);

        verify(pantryItemRepository).delete(item);
    }

    @Test
    void deleteItem_otherUsersItem_throwsNotFoundAndDoesNotDelete() {
        when(pantryItemRepository.findByIdAndUserId(ITEM_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteItem(OTHER_USER_ID, ITEM_ID))
                .isInstanceOf(NotFoundException.class);

        verify(pantryItemRepository, never()).delete(any(PantryItem.class));
    }

    @Test
    void listExpiringItems_nullDays_usesDefaultSevenDayWindow() {
        LocalDate today = LocalDate.now();
        when(pantryItemRepository.findByUserIdAndExpiryDateBetweenOrderByExpiryDateAsc(
                eq(USER_ID), eq(today), eq(today.plusDays(7))))
                .thenReturn(List.of());

        assertThat(service.listExpiringItems(USER_ID, null)).isEmpty();

        verify(pantryItemRepository).findByUserIdAndExpiryDateBetweenOrderByExpiryDateAsc(
                USER_ID, today, today.plusDays(7));
    }

    @Test
    void listExpiringItems_customDays_usesGivenWindowAndMapsResults() {
        LocalDate today = LocalDate.now();
        PantryItem item = pantryItem(1L, USER_ID, "Yogurt", new BigDecimal("1"), today.plusDays(1));
        when(pantryItemRepository.findByUserIdAndExpiryDateBetweenOrderByExpiryDateAsc(
                USER_ID, today, today.plusDays(3)))
                .thenReturn(List.of(item));

        List<PantryItemResponse> resp = service.listExpiringItems(USER_ID, 3);

        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).name()).isEqualTo("Yogurt");
    }

    private static PantryItem pantryItem(Long id, Long userId, String name, BigDecimal qty, LocalDate expiry) {
        return PantryItem.builder()
                .id(id)
                .user(User.builder().id(userId).build())
                .name(name)
                .quantity(qty)
                .unit("u")
                .expiryDate(expiry)
                .build();
    }
}
