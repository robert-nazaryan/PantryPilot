package org.example.pantrypilot.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.dto.ConsumeQuantityRequest;
import org.example.pantrypilot.dto.CreatePantryItemRequest;
import org.example.pantrypilot.dto.PageResponse;
import org.example.pantrypilot.dto.PantryItemResponse;
import org.example.pantrypilot.dto.UpdatePantryItemRequest;
import org.example.pantrypilot.model.PantryItem;
import org.example.pantrypilot.model.User;
import org.example.pantrypilot.repository.PantryItemRepository;
import org.example.pantrypilot.repository.UserRepository;
import org.example.pantrypilot.service.exception.InsufficientQuantityException;
import org.example.pantrypilot.service.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PantryItemService {

    static final int MAX_PAGE_SIZE = 100;
    static final int DEFAULT_PAGE_SIZE = 20;
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
    public PageResponse<PantryItemResponse> listItems(Long userId, Pageable pageable) {
        Pageable capped = capPageable(pageable);
        Page<PantryItem> page = pantryItemRepository.findByUserIdOrderByExpiryDateAscNullsLast(
                userId, capped);
        return PageResponse.from(page, PantryItemResponse::from);
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
        LocalDate end = LocalDate.now().plusDays(window);
        return pantryItemRepository
                .findByUserIdAndExpiryDateLessThanEqualOrderByExpiryDateAsc(userId, end)
                .stream()
                .map(PantryItemResponse::from)
                .toList();
    }

    private PantryItem loadOwnedItem(Long userId, Long itemId) {
        return pantryItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new NotFoundException("Pantry item not found"));
    }

    private static Pageable capPageable(Pageable pageable) {
        int size = pageable.getPageSize();
        if (size <= 0) {
            size = DEFAULT_PAGE_SIZE;
        } else if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }
        return PageRequest.of(Math.max(pageable.getPageNumber(), 0), size);
    }
}
