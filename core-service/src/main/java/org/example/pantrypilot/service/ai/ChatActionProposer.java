package org.example.pantrypilot.service.ai;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.pantrypilot.dto.AddRecipeIngredientActionPayload;
import org.example.pantrypilot.dto.AddShoppingListItemActionPayload;
import org.example.pantrypilot.dto.BulkActionPayload;
import org.example.pantrypilot.dto.ConsumePantryItemActionPayload;
import org.example.pantrypilot.dto.CreatePantryItemRequest;
import org.example.pantrypilot.dto.CreateRecipeActionPayload;
import org.example.pantrypilot.dto.CreateShoppingListActionPayload;
import org.example.pantrypilot.dto.DeletePantryItemActionPayload;
import org.example.pantrypilot.dto.DeleteRecipeActionPayload;
import org.example.pantrypilot.dto.GenerateShoppingListFromRecipeActionPayload;
import org.example.pantrypilot.dto.ProposedActionResponse;
import org.example.pantrypilot.dto.RemoveRecipeIngredientActionPayload;
import org.example.pantrypilot.dto.RemoveShoppingListItemActionPayload;
import org.example.pantrypilot.dto.SetShoppingListItemCheckedActionPayload;
import org.example.pantrypilot.dto.UpdatePantryItemActionPayload;
import org.example.pantrypilot.model.ChatAction;
import org.example.pantrypilot.model.ChatActionType;
import org.example.pantrypilot.model.ChatSession;
import org.example.pantrypilot.model.PantryItem;
import org.example.pantrypilot.model.Recipe;
import org.example.pantrypilot.model.RecipeIngredient;
import org.example.pantrypilot.model.ShoppingList;
import org.example.pantrypilot.model.ShoppingListItem;
import org.example.pantrypilot.repository.PantryItemRepository;
import org.example.pantrypilot.repository.RecipeIngredientRepository;
import org.example.pantrypilot.repository.RecipeRepository;
import org.example.pantrypilot.repository.ShoppingListRepository;
import org.example.pantrypilot.service.ChatActionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import static org.example.pantrypilot.service.ai.ProposerHelpers.bigDecimalArg;
import static org.example.pantrypilot.service.ai.ProposerHelpers.intArg;
import static org.example.pantrypilot.service.ai.ProposerHelpers.longArg;
import static org.example.pantrypilot.service.ai.ProposerHelpers.nullableLocalDateArg;
import static org.example.pantrypilot.service.ai.ProposerHelpers.nullableStringArg;
import static org.example.pantrypilot.service.ai.ProposerHelpers.stringArg;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatActionProposer {

    private static final int MAX_BULK_TARGETS = 200;

    private final ChatActionService actionService;
    private final PantryItemNameResolver nameResolver;
    private final PantryItemRepository pantryItemRepository;
    private final ShoppingListRepository shoppingListRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final ObjectMapper objectMapper;

    public record Outcome(ProposedActionResponse proposedAction, String clarificationText) {
        public static Outcome none() {
            return new Outcome(null, null);
        }

        public static Outcome proposed(ProposedActionResponse action) {
            return new Outcome(action, null);
        }

        public static Outcome clarify(String text) {
            return new Outcome(null, text);
        }
    }

    public Outcome propose(Long userId, ChatSession session, AiFunctionCall call) {
        return switch (call.name()) {
            case GeminiProvider.TOOL_CREATE_PANTRY_ITEM -> proposeCreatePantryItem(session, call);
            case GeminiProvider.TOOL_UPDATE_PANTRY_ITEM -> proposeUpdatePantryItem(userId, session, call);
            case GeminiProvider.TOOL_DELETE_PANTRY_ITEM -> proposeDeletePantryItem(userId, session, call);
            case GeminiProvider.TOOL_CONSUME_PANTRY_ITEM -> proposeConsumePantryItem(userId, session, call);
            case GeminiProvider.TOOL_BULK_DELETE_PANTRY_ITEMS -> proposeBulkDeletePantryItems(userId, session, call);
            case GeminiProvider.TOOL_CREATE_SHOPPING_LIST -> proposeCreateShoppingList(session, call);
            case GeminiProvider.TOOL_ADD_SHOPPING_LIST_ITEM -> proposeAddShoppingListItem(userId, session, call);
            case GeminiProvider.TOOL_REMOVE_SHOPPING_LIST_ITEM -> proposeRemoveShoppingListItem(userId, session, call);
            case GeminiProvider.TOOL_CHECK_SHOPPING_LIST_ITEM -> proposeSetShoppingListItemChecked(userId, session, call, true);
            case GeminiProvider.TOOL_UNCHECK_SHOPPING_LIST_ITEM -> proposeSetShoppingListItemChecked(userId, session, call, false);
            case GeminiProvider.TOOL_GENERATE_SHOPPING_LIST_FROM_RECIPE -> proposeGenerateShoppingListFromRecipe(userId, session, call);
            case GeminiProvider.TOOL_CREATE_RECIPE -> proposeCreateRecipe(session, call);
            case GeminiProvider.TOOL_DELETE_RECIPE -> proposeDeleteRecipe(userId, session, call);
            case GeminiProvider.TOOL_ADD_RECIPE_INGREDIENT -> proposeAddRecipeIngredient(userId, session, call);
            case GeminiProvider.TOOL_REMOVE_RECIPE_INGREDIENT -> proposeRemoveRecipeIngredient(userId, session, call);
            default -> {
                log.warn("Ignoring unknown function call from model: {}", call.name());
                yield Outcome.none();
            }
        };
    }

    // ---------- Pantry ----------

    private Outcome proposeCreatePantryItem(ChatSession session, AiFunctionCall call) {
        CreatePantryItemRequest payload = convert(call, CreatePantryItemRequest.class);
        if (payload == null) {
            return Outcome.none();
        }
        return persist(session, ChatActionType.CREATE_PANTRY_ITEM, payload);
    }

    private Outcome proposeUpdatePantryItem(Long userId, ChatSession session, AiFunctionCall call) {
        String name = stringArg(call, "name");
        PantryItemNameResolver.Result resolution = nameResolver.resolve(userId, name);
        Outcome clarification = clarificationOrNull(name, resolution);
        if (clarification != null) {
            return clarification;
        }
        PantryItem existing = resolution.item();
        UpdatePantryItemActionPayload payload = new UpdatePantryItemActionPayload(
                existing.getId(),
                existing.getName(),
                bigDecimalArg(call, "quantity", existing.getQuantity()),
                stringArg(call, "unit", existing.getUnit()),
                nullableStringArg(call, "category", existing.getCategory()),
                nullableLocalDateArg(call, "expiryDate",
                        existing.getExpiryDate() == null ? null : existing.getExpiryDate().toString()));
        return persist(session, ChatActionType.UPDATE_PANTRY_ITEM, payload);
    }

    private Outcome proposeDeletePantryItem(Long userId, ChatSession session, AiFunctionCall call) {
        String name = stringArg(call, "name");
        PantryItemNameResolver.Result resolution = nameResolver.resolve(userId, name);
        Outcome clarification = clarificationOrNull(name, resolution);
        if (clarification != null) {
            return clarification;
        }
        PantryItem existing = resolution.item();
        return persist(session, ChatActionType.DELETE_PANTRY_ITEM,
                new DeletePantryItemActionPayload(existing.getId(), existing.getName()));
    }

    private Outcome proposeConsumePantryItem(Long userId, ChatSession session, AiFunctionCall call) {
        String name = stringArg(call, "name");
        PantryItemNameResolver.Result resolution = nameResolver.resolve(userId, name);
        Outcome clarification = clarificationOrNull(name, resolution);
        if (clarification != null) {
            return clarification;
        }
        PantryItem existing = resolution.item();
        BigDecimal amount = bigDecimalArg(call, "quantity", null);
        if (amount == null || amount.signum() <= 0) {
            log.warn("consume_pantry_item missing/invalid quantity: {}", call.args());
            return Outcome.none();
        }
        return persist(session, ChatActionType.CONSUME_PANTRY_ITEM,
                new ConsumePantryItemActionPayload(
                        existing.getId(), existing.getName(), amount,
                        existing.getUnit(), existing.getQuantity()));
    }

    private Outcome proposeBulkDeletePantryItems(Long userId, ChatSession session, AiFunctionCall call) {
        String scope = stringArg(call, "scope", "all");
        List<PantryItem> targets;
        String summary;
        if ("byName".equalsIgnoreCase(scope)) {
            String nameFilter = stringArg(call, "nameFilter");
            if (nameFilter == null || nameFilter.isBlank()) {
                return Outcome.none();
            }
            targets = pantryItemRepository.findByUserIdAndNameIgnoreCase(userId, nameFilter);
            summary = "Remove all pantry items named \"" + nameFilter + "\"";
        } else {
            targets = pantryItemRepository.findByUserIdOrderByExpiryDateAscNullsLast(
                    userId, PageRequest.of(0, MAX_BULK_TARGETS)).getContent();
            summary = "Empty pantry (" + targets.size() + " items)";
        }
        if (targets.isEmpty()) {
            return Outcome.clarify("Nothing to remove — no matching pantry items.");
        }
        List<Object> targetPayloads = targets.stream()
                .<Object>map(p -> new DeletePantryItemActionPayload(p.getId(), p.getName()))
                .toList();
        BulkActionPayload payload = new BulkActionPayload(
                ChatActionType.DELETE_PANTRY_ITEM, targetPayloads, summary);
        return persist(session, ChatActionType.BULK_ACTION, payload);
    }

    // ---------- Shopping lists ----------

    private Outcome proposeCreateShoppingList(ChatSession session, AiFunctionCall call) {
        String name = nullableStringArg(call, "name", null);
        return persist(session, ChatActionType.CREATE_SHOPPING_LIST,
                new CreateShoppingListActionPayload(name));
    }

    private Outcome proposeAddShoppingListItem(Long userId, ChatSession session, AiFunctionCall call) {
        ShoppingList list = resolveShoppingListFromCall(userId, call);
        if (list == null) {
            return listClarification(call, "add an item to");
        }
        String name = stringArg(call, "name");
        if (name == null || name.isBlank()) {
            return Outcome.none();
        }
        return persist(session, ChatActionType.ADD_SHOPPING_LIST_ITEM,
                new AddShoppingListItemActionPayload(
                        list.getId(), list.getName(), name,
                        bigDecimalArg(call, "quantity", null),
                        nullableStringArg(call, "unit", null)));
    }

    private Outcome proposeRemoveShoppingListItem(Long userId, ChatSession session, AiFunctionCall call) {
        ShoppingList list = resolveShoppingListFromCall(userId, call);
        if (list == null) {
            return listClarification(call, "remove an item from");
        }
        ShoppingListItem item = resolveShoppingListItem(list, stringArg(call, "itemName"),
                longArg(call, "itemId"));
        if (item == null) {
            return Outcome.clarify("Which item on \"" + list.getName() + "\" did you mean?");
        }
        return persist(session, ChatActionType.REMOVE_SHOPPING_LIST_ITEM,
                new RemoveShoppingListItemActionPayload(
                        list.getId(), list.getName(), item.getId(), item.getName()));
    }

    private Outcome proposeSetShoppingListItemChecked(
            Long userId, ChatSession session, AiFunctionCall call, boolean checked) {
        ShoppingList list = resolveShoppingListFromCall(userId, call);
        if (list == null) {
            return listClarification(call, checked ? "check off items on" : "uncheck items on");
        }
        ShoppingListItem item = resolveShoppingListItem(list, stringArg(call, "itemName"),
                longArg(call, "itemId"));
        if (item == null) {
            return Outcome.clarify("Which item on \"" + list.getName() + "\" did you mean?");
        }
        ChatActionType type = checked
                ? ChatActionType.CHECK_SHOPPING_LIST_ITEM
                : ChatActionType.UNCHECK_SHOPPING_LIST_ITEM;
        return persist(session, type, new SetShoppingListItemCheckedActionPayload(
                list.getId(), list.getName(), item.getId(), item.getName(), checked));
    }

    private Outcome proposeGenerateShoppingListFromRecipe(
            Long userId, ChatSession session, AiFunctionCall call) {
        Recipe recipe = resolveRecipeFromCall(userId, call);
        if (recipe == null) {
            return Outcome.clarify("Which recipe should I use to generate the shopping list?");
        }
        return persist(session, ChatActionType.GENERATE_SHOPPING_LIST_FROM_RECIPE,
                new GenerateShoppingListFromRecipeActionPayload(recipe.getId(), recipe.getTitle()));
    }

    // ---------- Recipes ----------

    private Outcome proposeCreateRecipe(ChatSession session, AiFunctionCall call) {
        String title = stringArg(call, "title");
        String instructions = stringArg(call, "instructions");
        if (title == null || instructions == null || title.isBlank() || instructions.isBlank()) {
            log.warn("create_recipe missing title/instructions: {}", call.args());
            return Outcome.none();
        }
        Object tagsObj = call.args().get("tags");
        List<String> tags = tagsObj instanceof List<?> raw
                ? raw.stream().map(String::valueOf).toList()
                : List.of();
        return persist(session, ChatActionType.CREATE_RECIPE,
                new CreateRecipeActionPayload(title, instructions, intArg(call, "cookTimeMinutes"), tags));
    }

    private Outcome proposeDeleteRecipe(Long userId, ChatSession session, AiFunctionCall call) {
        Recipe recipe = resolveRecipeFromCall(userId, call);
        if (recipe == null) {
            return Outcome.clarify("Which recipe did you want to delete?");
        }
        return persist(session, ChatActionType.DELETE_RECIPE,
                new DeleteRecipeActionPayload(recipe.getId(), recipe.getTitle()));
    }

    private Outcome proposeAddRecipeIngredient(Long userId, ChatSession session, AiFunctionCall call) {
        Recipe recipe = resolveRecipeFromCall(userId, call);
        if (recipe == null) {
            return Outcome.clarify("Which recipe should I add the ingredient to?");
        }
        String name = stringArg(call, "name");
        BigDecimal qty = bigDecimalArg(call, "quantity", null);
        String unit = nullableStringArg(call, "unit", null);
        if (name == null || qty == null || unit == null) {
            return Outcome.none();
        }
        return persist(session, ChatActionType.ADD_RECIPE_INGREDIENT,
                new AddRecipeIngredientActionPayload(
                        recipe.getId(), recipe.getTitle(), name, qty, unit));
    }

    private Outcome proposeRemoveRecipeIngredient(Long userId, ChatSession session, AiFunctionCall call) {
        Recipe recipe = resolveRecipeFromCall(userId, call);
        if (recipe == null) {
            return Outcome.clarify("Which recipe's ingredient should I remove?");
        }
        Long ingredientId = longArg(call, "ingredientId");
        String ingredientName = stringArg(call, "ingredientName");
        RecipeIngredient ingredient = null;
        if (ingredientId != null) {
            ingredient = recipeIngredientRepository.findByIdAndRecipeId(ingredientId, recipe.getId())
                    .orElse(null);
        }
        if (ingredient == null && ingredientName != null && !ingredientName.isBlank()) {
            List<RecipeIngredient> all = recipeIngredientRepository.findByRecipeId(recipe.getId());
            List<RecipeIngredient> matches = all.stream()
                    .filter(i -> i.getName().equalsIgnoreCase(ingredientName))
                    .toList();
            if (matches.size() == 1) {
                ingredient = matches.get(0);
            }
        }
        if (ingredient == null) {
            return Outcome.clarify("Which ingredient on \"" + recipe.getTitle() + "\" did you mean?");
        }
        return persist(session, ChatActionType.REMOVE_RECIPE_INGREDIENT,
                new RemoveRecipeIngredientActionPayload(
                        recipe.getId(), recipe.getTitle(),
                        ingredient.getId(), ingredient.getName()));
    }

    // ---------- Helpers ----------

    private ShoppingList resolveShoppingListFromCall(Long userId, AiFunctionCall call) {
        Long listId = longArg(call, "listId");
        if (listId != null) {
            return shoppingListRepository.findByIdAndUserId(listId, userId).orElse(null);
        }
        String listName = stringArg(call, "listName");
        if (listName != null && !listName.isBlank()) {
            List<ShoppingList> matches = shoppingListRepository.findByUserIdAndNameIgnoreCase(
                    userId, listName);
            if (matches.size() == 1) {
                return matches.get(0);
            }
        }
        return null;
    }

    private ShoppingListItem resolveShoppingListItem(ShoppingList list, String itemName, Long itemId) {
        ShoppingList loaded = shoppingListRepository.findByIdAndUserIdWithItems(
                list.getId(), list.getUser().getId()).orElse(list);
        List<ShoppingListItem> items = loaded.getItems();
        if (items == null || items.isEmpty()) {
            return null;
        }
        if (itemId != null) {
            for (ShoppingListItem i : items) {
                if (itemId.equals(i.getId())) {
                    return i;
                }
            }
        }
        if (itemName != null && !itemName.isBlank()) {
            List<ShoppingListItem> matches = items.stream()
                    .filter(i -> i.getName().equalsIgnoreCase(itemName))
                    .toList();
            if (matches.size() == 1) {
                return matches.get(0);
            }
        }
        return null;
    }

    private Recipe resolveRecipeFromCall(Long userId, AiFunctionCall call) {
        Long recipeId = longArg(call, "recipeId");
        if (recipeId != null) {
            return recipeRepository.findByIdAndUserId(recipeId, userId).orElse(null);
        }
        String title = stringArg(call, "recipeTitle");
        if (title == null) {
            title = stringArg(call, "title");
        }
        if (title != null && !title.isBlank()) {
            List<Recipe> matches = recipeRepository.findByUserIdAndTitleIgnoreCase(userId, title);
            if (matches.size() == 1) {
                return matches.get(0);
            }
        }
        return null;
    }

    private Outcome listClarification(AiFunctionCall call, String verbPhrase) {
        String requested = stringArg(call, "listName", "that shopping list");
        return Outcome.clarify("I couldn't find \"" + requested + "\" to " + verbPhrase
                + ". Which shopping list did you mean?");
    }

    private Outcome clarificationOrNull(String requestedName, PantryItemNameResolver.Result r) {
        return switch (r.outcome()) {
            case FOUND -> null;
            case NOT_FOUND -> Outcome.clarify(
                    "I don't see \"" + requestedName + "\" in your pantry. Which item did you mean? "
                            + "(If you meant to remove several with this name, ask to remove all of them.)");
            case AMBIGUOUS -> {
                String candidates = r.candidates().stream()
                        .map(i -> i.getQuantity() + " " + i.getUnit())
                        .collect(Collectors.joining(", "));
                yield Outcome.clarify("You have multiple items called \"" + requestedName + "\": "
                        + candidates + ". Which one? "
                        + "(Say \"both\" or \"all\" to include every match.)");
            }
        };
    }

    private Outcome persist(ChatSession session, ChatActionType type, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            log.warn("Failed to serialize chat action payload for {}", type, ex);
            return Outcome.none();
        }
        ChatAction action = actionService.propose(session, type, json);
        return Outcome.proposed(new ProposedActionResponse(
                action.getId(), action.getType(), action.getStatus(), payload));
    }

    private <T> T convert(AiFunctionCall call, Class<T> type) {
        try {
            return objectMapper.convertValue(call.args(), type);
        } catch (IllegalArgumentException ex) {
            log.warn("Model returned unusable {} args: {} — {}", call.name(), call.args(), ex.getMessage());
            return null;
        }
    }
}
