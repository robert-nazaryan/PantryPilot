package org.example.pantrypilot.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PantryItemService {

    private static final int DEFAULT_EXPIRING_DAYS = 7;

    private final PantryItemRepository pantryItemRepository;
    private final UserRepository userRepository;

    @Transactional
    public PantryItemResponse createItem(Long userId, CreatePantryItemRequest req) {
        User owner = userRepository.getReferenceById(userId);
        PantryItem item = PantryItem.builder()
                .user(owner)
                .name(req.name())
                .quantity(req.quantity())
                .unit(req.unit())
                .category(req.category())
                .categorySource(req.category() != null ? "user" : null)
                .expiryDate(req.expiryDate())
                .build();
        return PantryItemResponse.from(pantryItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<PantryItemResponse> listItems(Long userId) {
        return pantryItemRepository.findByUserIdOrderByExpiryDateAscNullsLast(userId).stream()
                .map(PantryItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PantryItemResponse getItem(Long userId, Long itemId) {
        return PantryItemResponse.from(loadOwnedItem(userId, itemId));
    }

    @Transactional
    public PantryItemResponse updateItem(Long userId, Long itemId, UpdatePantryItemRequest req) {
        PantryItem item = loadOwnedItem(userId, itemId);
        item.setName(req.name());
        item.setQuantity(req.quantity());
        item.setUnit(req.unit());
        item.setCategory(req.category());
        item.setCategorySource(req.category() != null ? "user" : null);
        item.setExpiryDate(req.expiryDate());
        return PantryItemResponse.from(pantryItemRepository.save(item));
    }

    @Transactional
    public PantryItemResponse consumeQuantity(Long userId, Long itemId, ConsumeQuantityRequest req) {
        PantryItem item = loadOwnedItem(userId, itemId);
        BigDecimal remaining = item.getQuantity().subtract(req.quantity());
        if (remaining.signum() < 0) {
            throw new InsufficientQuantityException(
                    "Consume quantity exceeds available quantity");
        }
        item.setQuantity(remaining);
        return PantryItemResponse.from(pantryItemRepository.save(item));
    }

    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        PantryItem item = loadOwnedItem(userId, itemId);
        pantryItemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public List<PantryItemResponse> listExpiringItems(Long userId, Integer days) {
        int window = (days == null || days < 0) ? DEFAULT_EXPIRING_DAYS : days;
        LocalDate today = LocalDate.now();
        return pantryItemRepository
                .findByUserIdAndExpiryDateBetweenOrderByExpiryDateAsc(userId, today, today.plusDays(window))
                .stream()
                .map(PantryItemResponse::from)
                .toList();
    }

    private PantryItem loadOwnedItem(Long userId, Long itemId) {
        return pantryItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new NotFoundException("Pantry item not found"));
    }
}
