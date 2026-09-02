package org.example.pantrypilot.service.ai;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.model.PantryItem;
import org.example.pantrypilot.repository.PantryItemRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PantryItemNameResolver {

    private final PantryItemRepository pantryItemRepository;

    public enum Outcome { FOUND, NOT_FOUND, AMBIGUOUS }

    public record Result(Outcome outcome, PantryItem item, List<PantryItem> candidates) {
        public static Result notFound() {
            return new Result(Outcome.NOT_FOUND, null, List.of());
        }

        public static Result found(PantryItem item) {
            return new Result(Outcome.FOUND, item, List.of(item));
        }

        public static Result ambiguous(List<PantryItem> candidates) {
            return new Result(Outcome.AMBIGUOUS, null, List.copyOf(candidates));
        }
    }

    @Transactional(readOnly = true)
    public Result resolve(Long userId, String name) {
        if (name == null || name.isBlank()) {
            return Result.notFound();
        }
        List<PantryItem> matches = pantryItemRepository.findByUserIdAndNameIgnoreCase(userId, name.trim());
        if (matches.isEmpty()) {
            return Result.notFound();
        }
        if (matches.size() > 1) {
            return Result.ambiguous(matches);
        }
        return Result.found(matches.get(0));
    }
}
