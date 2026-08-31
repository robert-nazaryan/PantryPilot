package org.example.pantrypilot.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.example.pantrypilot.event.PantryItemExpiringEvent;
import org.example.pantrypilot.model.PantryItem;
import org.example.pantrypilot.model.User;
import org.example.pantrypilot.repository.PantryItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpiringDigestServiceTest {

    @Mock private PantryItemRepository pantryItemRepository;
    @InjectMocks private ExpiringDigestService service;

    @Test
    void buildDigests_emptyResult_returnsEmptyList() {
        when(pantryItemRepository.findAllExpiringByOwner(any())).thenReturn(List.of());

        assertThat(service.buildDigests(3)).isEmpty();
    }

    @Test
    void buildDigests_multipleUsers_returnsOneEventPerUser() {
        User u1 = user(1L, "alice@example.com", "Alice");
        User u2 = user(2L, "bob@example.com", null);
        LocalDate today = LocalDate.now();
        when(pantryItemRepository.findAllExpiringByOwner(any())).thenReturn(List.of(
                item(u1, "Milk", "L", today.plusDays(1)),
                item(u1, "Bread", "loaf", today.plusDays(2)),
                item(u2, "Yogurt", "cup", today.plusDays(3))));

        List<PantryItemExpiringEvent> events = service.buildDigests(3);

        assertThat(events).hasSize(2);
        assertThat(events).extracting(PantryItemExpiringEvent::userId).containsExactly(1L, 2L);
        PantryItemExpiringEvent alice = events.get(0);
        assertThat(alice.email()).isEqualTo("alice@example.com");
        assertThat(alice.displayName()).isEqualTo("Alice");
        assertThat(alice.items()).hasSize(2)
                .extracting(PantryItemExpiringEvent.ExpiringItem::name)
                .containsExactly("Milk", "Bread");
        assertThat(alice.eventId()).isNotNull();
        assertThat(alice.occurredAt()).isNotNull();
        assertThat(events.get(1).items()).hasSize(1);
    }

    private static User user(Long id, String email, String displayName) {
        return User.builder().id(id).email(email).displayName(displayName).build();
    }

    private static PantryItem item(User owner, String name, String unit, LocalDate expiry) {
        return PantryItem.builder()
                .user(owner)
                .name(name)
                .quantity(BigDecimal.ONE)
                .unit(unit)
                .expiryDate(expiry)
                .build();
    }
}
