package org.example.pantrypilot.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.event.PantryItemExpiringEvent;
import org.example.pantrypilot.event.PantryItemExpiringEvent.ExpiringItem;
import org.example.pantrypilot.model.PantryItem;
import org.example.pantrypilot.model.User;
import org.example.pantrypilot.repository.PantryItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpiringDigestService {

    private final PantryItemRepository pantryItemRepository;

    @Transactional(readOnly = true)
    public List<PantryItemExpiringEvent> buildDigests(int windowDays) {
        LocalDate end = LocalDate.now().plusDays(windowDays);
        List<PantryItem> all = pantryItemRepository.findAllExpiringByOwner(end);
        Map<Long, List<PantryItem>> byUser = groupByUser(all);

        List<PantryItemExpiringEvent> events = new ArrayList<>(byUser.size());
        for (Map.Entry<Long, List<PantryItem>> entry : byUser.entrySet()) {
            List<PantryItem> items = entry.getValue();
            User owner = items.get(0).getUser();
            List<ExpiringItem> payload = items.stream()
                    .map(item -> new ExpiringItem(
                            item.getName(), item.getQuantity(), item.getUnit(), item.getExpiryDate()))
                    .toList();
            events.add(PantryItemExpiringEvent.now(
                    owner.getId(), owner.getEmail(), owner.getDisplayName(), payload));
        }
        return events;
    }

    private static Map<Long, List<PantryItem>> groupByUser(List<PantryItem> items) {
        Map<Long, List<PantryItem>> byUser = new LinkedHashMap<>();
        for (PantryItem item : items) {
            byUser.computeIfAbsent(item.getUser().getId(), k -> new ArrayList<>()).add(item);
        }
        return byUser;
    }
}
