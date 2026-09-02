package org.example.pantrypilot.service.ai;

import java.util.List;
import java.util.Map;

/**
 * Static holder for Gemini function-calling tool declarations. Extracted from GeminiProvider so
 * each domain (pantry / shopping list / recipe / bulk) can be described in one focused place.
 */
final class ToolDeclarations {

    private ToolDeclarations() {
    }

    static List<Map<String, Object>> all() {
        return List.of(
                createPantryItem(),
                updatePantryItem(),
                deletePantryItem(),
                consumePantryItem(),
                bulkDeletePantryItems(),
                createShoppingList(),
                addShoppingListItem(),
                removeShoppingListItem(),
                checkShoppingListItem(),
                uncheckShoppingListItem(),
                generateShoppingListFromRecipe(),
                createRecipe(),
                deleteRecipe(),
                addRecipeIngredient(),
                removeRecipeIngredient());
    }

    // ---------- Pantry ----------

    private static Map<String, Object> createPantryItem() {
        return decl(GeminiProvider.TOOL_CREATE_PANTRY_ITEM,
                "Propose adding a single item to the user's pantry (NOT to a shopping list). "
                        + "Use ONLY when the user clearly asks to add/save/log/track a pantry item. "
                        + "For shopping list items use add_shopping_list_item instead. "
                        + "The user will see a confirmation card and must click Confirm before the "
                        + "item is actually created.",
                Map.of(
                        "name", str("Name of the item, e.g. 'Milk' or 'Whole wheat flour'."),
                        "quantity", num("Positive numeric quantity, e.g. 2 or 0.5."),
                        "unit", str("Unit of measurement. Common values: pcs, g, kg, ml, l, tsp, tbsp, cup, oz, lb."),
                        "category", str("Optional category. Common values: dairy, produce, meat, grains, spices, frozen, bakery, other."),
                        "expiryDate", str("Optional expiry date in ISO format YYYY-MM-DD. Only include if the user gave one explicitly.")),
                List.of("name", "quantity", "unit"));
    }

    private static Map<String, Object> updatePantryItem() {
        return decl(GeminiProvider.TOOL_UPDATE_PANTRY_ITEM,
                "Propose updating fields on an existing pantry item. Identify by current name "
                        + "(case-insensitive exact match against the pantry listed in the system context). "
                        + "Provide only the fields that should change; omitted fields keep current values.",
                Map.of(
                        "name", str("The current name of the pantry item to update."),
                        "quantity", num("New quantity (>= 0). Omit if unchanged."),
                        "unit", str("New unit. Omit if unchanged."),
                        "category", str("New category. Omit if unchanged."),
                        "expiryDate", str("New expiry date in ISO YYYY-MM-DD. Omit if unchanged.")),
                List.of("name"));
    }

    private static Map<String, Object> deletePantryItem() {
        return decl(GeminiProvider.TOOL_DELETE_PANTRY_ITEM,
                "Propose removing an ONE existing pantry item entirely. For removing multiple items "
                        + "or emptying the pantry, use bulk_delete_pantry_items instead. Identify by current name.",
                Map.of("name", str("The current name of the pantry item to delete.")),
                List.of("name"));
    }

    private static Map<String, Object> consumePantryItem() {
        return decl(GeminiProvider.TOOL_CONSUME_PANTRY_ITEM,
                "Propose reducing the quantity of an existing pantry item (partial consumption). Use "
                        + "when the user says they used/ate/drank/consumed part of an item. The 'quantity' "
                        + "argument is how much to consume, not the remaining amount.",
                Map.of(
                        "name", str("The current name of the pantry item to consume from."),
                        "quantity", num("How much to subtract (positive, must not exceed available).")),
                List.of("name", "quantity"));
    }

    private static Map<String, Object> bulkDeletePantryItems() {
        return decl(GeminiProvider.TOOL_BULK_DELETE_PANTRY_ITEMS,
                "Propose removing MULTIPLE pantry items in one batch. Use whenever the user asks to "
                        + "empty/clear/remove-all/etc. or when they answer 'both' / 'all of them' to a "
                        + "clarifying question about duplicate names. This produces ONE confirmation "
                        + "card listing everything to be removed. Do NOT loop through delete_pantry_item "
                        + "one at a time.",
                Map.of(
                        "scope", str("Either 'all' (delete every pantry item the user owns) or 'byName' "
                                + "(delete every item whose name matches nameFilter, case-insensitive). "
                                + "Use 'byName' when the user answered 'both'/'all' to a name-ambiguity "
                                + "clarification."),
                        "nameFilter", str("Required only when scope='byName'. The item name to match.")),
                List.of("scope"));
    }

    // ---------- Shopping lists ----------

    private static Map<String, Object> createShoppingList() {
        return decl(GeminiProvider.TOOL_CREATE_SHOPPING_LIST,
                "Propose creating a new empty shopping list. Use when the user asks to start/create a list.",
                Map.of("name", str("Optional list name. If omitted the system uses 'Shopping List'.")),
                List.of());
    }

    private static Map<String, Object> addShoppingListItem() {
        return decl(GeminiProvider.TOOL_ADD_SHOPPING_LIST_ITEM,
                "Propose adding an item to an existing shopping list. Use when the user asks to add "
                        + "something TO A SHOPPING LIST (not to their pantry). Prefer listId if you can "
                        + "read it from the system context; otherwise pass listName. If no list exists yet "
                        + "the user may need to create one first — ask or propose create_shopping_list.",
                Map.of(
                        "listId", num("The [id=N] shown for the target list in the system context."),
                        "listName", str("Exact name of the target list (used if listId is not provided)."),
                        "name", str("Name of the item to add, e.g. 'Pepperoni'."),
                        "quantity", num("Optional numeric quantity."),
                        "unit", str("Optional unit, e.g. 'g', 'pcs'.")),
                List.of("name"));
    }

    private static Map<String, Object> removeShoppingListItem() {
        return decl(GeminiProvider.TOOL_REMOVE_SHOPPING_LIST_ITEM,
                "Propose removing an item from a shopping list.",
                Map.of(
                        "listId", num("The [id=N] of the target list."),
                        "listName", str("Exact list name (if listId not given)."),
                        "itemId", num("The [id=N] of the item to remove."),
                        "itemName", str("Item name (if itemId not given).")),
                List.of());
    }

    private static Map<String, Object> checkShoppingListItem() {
        return decl(GeminiProvider.TOOL_CHECK_SHOPPING_LIST_ITEM,
                "Propose marking a shopping list item as checked/bought.",
                Map.of(
                        "listId", num("Target list id."),
                        "listName", str("Target list name (if id not given)."),
                        "itemId", num("Item id to check."),
                        "itemName", str("Item name (if id not given).")),
                List.of());
    }

    private static Map<String, Object> uncheckShoppingListItem() {
        return decl(GeminiProvider.TOOL_UNCHECK_SHOPPING_LIST_ITEM,
                "Propose marking a shopping list item as unchecked.",
                Map.of(
                        "listId", num("Target list id."),
                        "listName", str("Target list name (if id not given)."),
                        "itemId", num("Item id to uncheck."),
                        "itemName", str("Item name (if id not given).")),
                List.of());
    }

    private static Map<String, Object> generateShoppingListFromRecipe() {
        return decl(GeminiProvider.TOOL_GENERATE_SHOPPING_LIST_FROM_RECIPE,
                "Propose creating a new shopping list from an existing saved recipe's ingredients. "
                        + "Identify the recipe by recipeId (preferred) or recipeTitle. This will create a "
                        + "new list, not merge into an existing one.",
                Map.of(
                        "recipeId", num("The [id=N] of the recipe."),
                        "recipeTitle", str("Exact recipe title (if id not given).")),
                List.of());
    }

    // ---------- Recipes ----------

    private static Map<String, Object> createRecipe() {
        return decl(GeminiProvider.TOOL_CREATE_RECIPE,
                "Propose saving a new recipe. Ingredients are NOT part of this call — after the recipe "
                        + "is created, use add_recipe_ingredient separately for each ingredient.",
                Map.of(
                        "title", str("Recipe title."),
                        "instructions", str("Step-by-step instructions as free text."),
                        "cookTimeMinutes", num("Optional cook time in minutes."),
                        "tags", Map.of("type", "array",
                                "items", Map.of("type", "string"),
                                "description", "Optional list of tags, e.g. ['italian', 'quick'].")),
                List.of("title", "instructions"));
    }

    private static Map<String, Object> deleteRecipe() {
        return decl(GeminiProvider.TOOL_DELETE_RECIPE,
                "Propose deleting a saved recipe.",
                Map.of(
                        "recipeId", num("Recipe id."),
                        "recipeTitle", str("Recipe title (if id not given).")),
                List.of());
    }

    private static Map<String, Object> addRecipeIngredient() {
        return decl(GeminiProvider.TOOL_ADD_RECIPE_INGREDIENT,
                "Propose adding an ingredient to a saved recipe.",
                Map.of(
                        "recipeId", num("Recipe id."),
                        "recipeTitle", str("Recipe title (if id not given)."),
                        "name", str("Ingredient name."),
                        "quantity", num("Quantity."),
                        "unit", str("Unit of measurement.")),
                List.of("name", "quantity", "unit"));
    }

    private static Map<String, Object> removeRecipeIngredient() {
        return decl(GeminiProvider.TOOL_REMOVE_RECIPE_INGREDIENT,
                "Propose removing an ingredient from a saved recipe.",
                Map.of(
                        "recipeId", num("Recipe id."),
                        "recipeTitle", str("Recipe title (if id not given)."),
                        "ingredientId", num("Ingredient id."),
                        "ingredientName", str("Ingredient name (if id not given).")),
                List.of());
    }

    // ---------- Schema helpers ----------

    private static Map<String, Object> decl(String name, String description,
                                            Map<String, Object> properties, List<String> required) {
        return Map.of(
                "name", name,
                "description", description,
                "parameters", Map.of(
                        "type", "object",
                        "properties", properties,
                        "required", required));
    }

    private static Map<String, Object> str(String description) {
        return Map.of("type", "string", "description", description);
    }

    private static Map<String, Object> num(String description) {
        return Map.of("type", "number", "description", description);
    }
}
