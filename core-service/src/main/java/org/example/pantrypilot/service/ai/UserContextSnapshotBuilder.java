package org.example.pantrypilot.service.ai;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.model.PantryItem;
import org.example.pantrypilot.model.Recipe;
import org.example.pantrypilot.model.RecipeIngredient;
import org.example.pantrypilot.model.ShoppingList;
import org.example.pantrypilot.model.ShoppingListItem;
import org.example.pantrypilot.repository.PantryItemRepository;
import org.example.pantrypilot.repository.RecipeRepository;
import org.example.pantrypilot.repository.ShoppingListRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserContextSnapshotBuilder {

    private static final int MAX_PANTRY_ITEMS = 100;
    private static final int MAX_RECIPES = 30;
    private static final int MAX_INGREDIENTS_PER_RECIPE = 20;
    private static final int MAX_ACTIVE_SHOPPING_LISTS = 10;
    private static final int MAX_ITEMS_PER_LIST = 50;

    private final PantryItemRepository pantryItemRepository;
    private final RecipeRepository recipeRepository;
    private final ShoppingListRepository shoppingListRepository;

    @Transactional(readOnly = true)
    public String buildFor(Long userId) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("You are PantryPilot's in-app cooking and pantry assistant. ")
                .append("Answer questions using the user's real data below. ")
                .append("Be concise, practical, and only suggest recipes the user could plausibly make ")
                .append("from their pantry (or note what's missing). ")
                .append("Do not invent items the user does not own.\n\n");

        appendPantry(sb, userId);
        appendRecipes(sb, userId);
        appendShoppingLists(sb, userId);
        return sb.toString();
    }

    private void appendPantry(StringBuilder sb, Long userId) {
        sb.append("## Pantry\n");
        var page = pantryItemRepository.findByUserIdOrderByExpiryDateAscNullsLast(
                userId, PageRequest.of(0, MAX_PANTRY_ITEMS));
        List<PantryItem> items = page.getContent();
        if (items.isEmpty()) {
            sb.append("(empty)\n");
        } else {
            for (PantryItem item : items) {
                sb.append("- ").append(item.getName())
                        .append(" — ").append(item.getQuantity()).append(' ').append(item.getUnit());
                if (item.getExpiryDate() != null) {
                    sb.append(" (expires ").append(item.getExpiryDate()).append(')');
                }
                sb.append('\n');
            }
            if (page.getTotalElements() > items.size()) {
                sb.append("(+ ").append(page.getTotalElements() - items.size())
                        .append(" more not shown)\n");
            }
        }
        sb.append('\n');
    }

    private void appendRecipes(StringBuilder sb, Long userId) {
        sb.append("## Saved recipes\n");
        var page = recipeRepository.findByUserId(userId, PageRequest.of(0, MAX_RECIPES));
        List<Recipe> recipes = page.getContent();
        if (recipes.isEmpty()) {
            sb.append("(none)\n");
        } else {
            for (Recipe r : recipes) {
                sb.append("- ").append(r.getTitle());
                if (r.getCookTimeMinutes() != null) {
                    sb.append(" (").append(r.getCookTimeMinutes()).append(" min)");
                }
                if (r.getTags() != null && r.getTags().length > 0) {
                    sb.append(" [").append(String.join(", ", r.getTags())).append(']');
                }
                sb.append('\n');
                appendIngredients(sb, r);
            }
            if (page.getTotalElements() > recipes.size()) {
                sb.append("(+ ").append(page.getTotalElements() - recipes.size())
                        .append(" more not shown)\n");
            }
        }
        sb.append('\n');
    }

    private void appendIngredients(StringBuilder sb, Recipe recipe) {
        var loaded = recipeRepository.findByIdAndUserIdWithIngredients(
                recipe.getId(), recipe.getUser().getId()).orElse(recipe);
        List<RecipeIngredient> ings = loaded.getIngredients();
        if (ings == null || ings.isEmpty()) {
            return;
        }
        int shown = 0;
        for (RecipeIngredient ing : ings) {
            if (shown >= MAX_INGREDIENTS_PER_RECIPE) {
                sb.append("    (+ ").append(ings.size() - shown).append(" more ingredients)\n");
                break;
            }
            sb.append("    * ").append(ing.getName());
            if (ing.getQuantity() != null) {
                sb.append(" — ").append(ing.getQuantity());
                if (ing.getUnit() != null) {
                    sb.append(' ').append(ing.getUnit());
                }
            }
            sb.append('\n');
            shown++;
        }
    }

    private void appendShoppingLists(StringBuilder sb, Long userId) {
        sb.append("## Shopping lists\n");
        var page = shoppingListRepository.findByUserId(
                userId, PageRequest.of(0, MAX_ACTIVE_SHOPPING_LISTS));
        List<ShoppingList> lists = page.getContent();
        List<ShoppingList> active = lists.stream().filter(ShoppingList::isActive).toList();
        if (active.isEmpty()) {
            sb.append("(no active lists)\n");
            return;
        }
        for (ShoppingList list : active) {
            String name = list.getName() != null ? list.getName() : "(unnamed)";
            sb.append("- ").append(name).append('\n');
            appendListItems(sb, list);
        }
    }

    private void appendListItems(StringBuilder sb, ShoppingList list) {
        var loaded = shoppingListRepository.findByIdAndUserIdWithItems(
                list.getId(), list.getUser().getId()).orElse(list);
        List<ShoppingListItem> items = loaded.getItems();
        if (items == null || items.isEmpty()) {
            return;
        }
        int shown = 0;
        for (ShoppingListItem item : items) {
            if (shown >= MAX_ITEMS_PER_LIST) {
                sb.append("    (+ ").append(items.size() - shown).append(" more items)\n");
                break;
            }
            sb.append("    ").append(item.isChecked() ? "[x] " : "[ ] ").append(item.getName());
            if (item.getQuantity() != null) {
                sb.append(" — ").append(item.getQuantity());
                if (item.getUnit() != null) {
                    sb.append(' ').append(item.getUnit());
                }
            }
            sb.append('\n');
            shown++;
        }
    }
}
