package org.example.pantrypilot.service.ai;

import java.util.List;
import java.util.Map;

/**
 * Provider-agnostic tool-name constants and function-schema declarations. Each provider
 * (Gemini / Groq / …) consumes {@link #functionSchemas()} and wraps it in its own outer
 * request shape.
 */
public final class AiTools {

    public static final String TOOL_CREATE_PANTRY_ITEM = "create_pantry_item";
    public static final String TOOL_UPDATE_PANTRY_ITEM = "update_pantry_item";
    public static final String TOOL_DELETE_PANTRY_ITEM = "delete_pantry_item";
    public static final String TOOL_CONSUME_PANTRY_ITEM = "consume_pantry_item";
    public static final String TOOL_BULK_DELETE_PANTRY_ITEMS = "bulk_delete_pantry_items";
    public static final String TOOL_CREATE_SHOPPING_LIST = "create_shopping_list";
    public static final String TOOL_RENAME_SHOPPING_LIST = "rename_shopping_list";
    public static final String TOOL_DELETE_SHOPPING_LIST = "delete_shopping_list";
    public static final String TOOL_ADD_SHOPPING_LIST_ITEM = "add_shopping_list_item";
    public static final String TOOL_REMOVE_SHOPPING_LIST_ITEM = "remove_shopping_list_item";
    public static final String TOOL_CHECK_SHOPPING_LIST_ITEM = "check_shopping_list_item";
    public static final String TOOL_UNCHECK_SHOPPING_LIST_ITEM = "uncheck_shopping_list_item";
    public static final String TOOL_GENERATE_SHOPPING_LIST_FROM_RECIPE = "generate_shopping_list_from_recipe";
    public static final String TOOL_CREATE_RECIPE = "create_recipe";
    public static final String TOOL_UPDATE_RECIPE = "update_recipe";
    public static final String TOOL_DELETE_RECIPE = "delete_recipe";
    public static final String TOOL_ADD_RECIPE_INGREDIENT = "add_recipe_ingredient";
    public static final String TOOL_REMOVE_RECIPE_INGREDIENT = "remove_recipe_ingredient";

    private AiTools() {
    }

    public static List<Map<String, Object>> functionSchemas() {
        return List.of(
                createPantryItem(),
                updatePantryItem(),
                deletePantryItem(),
                consumePantryItem(),
                bulkDeletePantryItems(),
                createShoppingList(),
                renameShoppingList(),
                deleteShoppingList(),
                addShoppingListItem(),
                removeShoppingListItem(),
                checkShoppingListItem(),
                uncheckShoppingListItem(),
                generateShoppingListFromRecipe(),
                createRecipe(),
                updateRecipe(),
                deleteRecipe(),
                addRecipeIngredient(),
                removeRecipeIngredient());
    }

    private static Map<String, Object> createPantryItem() {
        return decl(TOOL_CREATE_PANTRY_ITEM,
                "Add ONE item to the user's PANTRY (things they physically have on hand). "
                        + "Use ONLY when the user says they HAVE / BOUGHT / OWN the item, or explicitly says "
                        + "\"add to pantry\". DO NOT use this when the user says \"need to buy\", \"put on the "
                        + "shopping list\", or \"add to my shopping list\" — those go to add_shopping_list_item. "
                        + "Example wording that matches: \"I got 2 kg of flour\", \"add whole milk to my pantry\", "
                        + "\"I have a dozen eggs now\". Confirmation card is shown before the item is created.",
                Map.of(
                        "name", str("Name of the item, e.g. 'Milk' or 'Whole wheat flour'."),
                        "quantity", num("Positive numeric quantity, e.g. 2 or 0.5."),
                        "unit", str("Unit of measurement. Common values: pcs, g, kg, ml, l, tsp, tbsp, cup, oz, lb."),
                        "category", str("Optional category. Common values: dairy, produce, meat, grains, spices, frozen, bakery, other."),
                        "expiryDate", str("Optional expiry date in ISO format YYYY-MM-DD. Only include if the user gave one explicitly.")),
                List.of("name", "quantity", "unit"));
    }

    private static Map<String, Object> updatePantryItem() {
        return decl(TOOL_UPDATE_PANTRY_ITEM,
                "Update fields on an EXISTING pantry item (something the user already HAS). "
                        + "Identify by current name (case-insensitive exact match against the [pantry:N] entries "
                        + "in the system context). Provide only the fields that should change; omitted fields "
                        + "keep current values. NOT for shopping-list items — use update / remove shopping-list "
                        + "tools instead.",
                Map.of(
                        "name", str("The current name of the pantry item to update."),
                        "quantity", num("New quantity (>= 0). Omit if unchanged."),
                        "unit", str("New unit. Omit if unchanged."),
                        "category", str("New category. Omit if unchanged."),
                        "expiryDate", str("New expiry date in ISO YYYY-MM-DD. Omit if unchanged.")),
                List.of("name"));
    }

    private static Map<String, Object> deletePantryItem() {
        return decl(TOOL_DELETE_PANTRY_ITEM,
                "Remove ONE existing pantry item entirely (something the user has, that they now no longer have). "
                        + "For removing multiple items or emptying the pantry, use bulk_delete_pantry_items. "
                        + "NOT for removing items from a shopping list — use remove_shopping_list_item instead. "
                        + "Identify by current name.",
                Map.of("name", str("The current name of the pantry item to delete.")),
                List.of("name"));
    }

    private static Map<String, Object> consumePantryItem() {
        return decl(TOOL_CONSUME_PANTRY_ITEM,
                "Reduce the quantity of an existing pantry item (partial consumption). Use when the user "
                        + "says they used / ate / drank / consumed part of an item they HAVE. The 'quantity' "
                        + "argument is HOW MUCH to consume, not the amount remaining.",
                Map.of(
                        "name", str("The current name of the pantry item to consume from."),
                        "quantity", num("How much to subtract (positive, must not exceed available).")),
                List.of("name", "quantity"));
    }

    private static Map<String, Object> bulkDeletePantryItems() {
        return decl(TOOL_BULK_DELETE_PANTRY_ITEMS,
                "Remove MULTIPLE pantry items in one batch. Use whenever the user asks to "
                        + "empty / clear / remove-all / etc., or when they answer 'both' / 'all of them' / "
                        + "'yes all' to a clarifying question about duplicate names. Produces ONE confirmation "
                        + "card listing everything to be removed. Do NOT loop through delete_pantry_item one "
                        + "at a time. Applies to PANTRY only, not shopping lists.",
                Map.of(
                        "scope", str("Either 'all' (delete every pantry item the user owns) or 'byName' "
                                + "(delete every item whose name matches nameFilter, case-insensitive). "
                                + "Use 'byName' when the user answered 'both'/'all' to a name-ambiguity "
                                + "clarification."),
                        "nameFilter", str("Required only when scope='byName'. The item name to match.")),
                List.of("scope"));
    }

    private static Map<String, Object> createShoppingList() {
        return decl(TOOL_CREATE_SHOPPING_LIST,
                "Create a new empty SHOPPING LIST (a to-buy list, separate from the pantry). "
                        + "Use when the user asks to start / create a NEW list. NOT for adding items — use "
                        + "add_shopping_list_item for that.",
                Map.of("name", str("Optional list name. If omitted the system uses 'Shopping List'.")),
                List.of());
    }

    private static Map<String, Object> renameShoppingList() {
        return decl(TOOL_RENAME_SHOPPING_LIST,
                "Rename an existing shopping list. Identify the target list by listId (preferred, from the "
                        + "[list:N] entries in the system context) or by its current listName.",
                Map.of(
                        "listId", num("The [list:N] id of the list to rename."),
                        "listName", str("Current exact list name (if listId not given)."),
                        "newName", str("New name for the list.")),
                List.of("newName"));
    }

    private static Map<String, Object> deleteShoppingList() {
        return decl(TOOL_DELETE_SHOPPING_LIST,
                "Delete an ENTIRE shopping list and all its items in one shot. Use only when the user wants "
                        + "to remove the whole list, not individual items. To remove a single item, use "
                        + "remove_shopping_list_item.",
                Map.of(
                        "listId", num("The [list:N] id of the list to delete."),
                        "listName", str("Exact list name (if listId not given).")),
                List.of());
    }

    private static Map<String, Object> addShoppingListItem() {
        return decl(TOOL_ADD_SHOPPING_LIST_ITEM,
                "Add an item TO A SHOPPING LIST — something the user still NEEDS TO BUY. "
                        + "Use when the user says \"need to buy\", \"put on the list\", \"add to shopping list\", "
                        + "\"remind me to buy\", or when the request is clearly about a shopping list (e.g. "
                        + "\"add pepperoni to my groceries list\"). DO NOT use this when the user says they "
                        + "already HAVE the item — that goes to create_pantry_item. Prefer listId (from the "
                        + "[list:N] entries above) if visible; otherwise pass listName. If no list exists yet "
                        + "the user may need to create one first — either ask or propose create_shopping_list.",
                Map.of(
                        "listId", num("The [list:N] id of the target list."),
                        "listName", str("Exact name of the target list (used if listId is not provided)."),
                        "name", str("Name of the item to add, e.g. 'Pepperoni'."),
                        "quantity", num("Optional numeric quantity."),
                        "unit", str("Optional unit, e.g. 'g', 'pcs'.")),
                List.of("name"));
    }

    private static Map<String, Object> removeShoppingListItem() {
        return decl(TOOL_REMOVE_SHOPPING_LIST_ITEM,
                "Remove one item from a shopping list (an item the user no longer needs to buy). "
                        + "NOT for pantry items — use delete_pantry_item for those.",
                Map.of(
                        "listId", num("The [list:N] id of the target list."),
                        "listName", str("Exact list name (if listId not given)."),
                        "itemId", num("The [list-item:N] id of the item to remove."),
                        "itemName", str("Item name (if itemId not given).")),
                List.of());
    }

    private static Map<String, Object> checkShoppingListItem() {
        return decl(TOOL_CHECK_SHOPPING_LIST_ITEM,
                "Mark a shopping list item as checked/bought. Use when the user says they picked it up, "
                        + "bought it, got it, etc. — but only if the intent is to mark it done on the list "
                        + "rather than move it to the pantry.",
                Map.of(
                        "listId", num("Target list id."),
                        "listName", str("Target list name (if id not given)."),
                        "itemId", num("Item id to check."),
                        "itemName", str("Item name (if id not given).")),
                List.of());
    }

    private static Map<String, Object> uncheckShoppingListItem() {
        return decl(TOOL_UNCHECK_SHOPPING_LIST_ITEM,
                "Mark a shopping list item as unchecked (still needs to be bought).",
                Map.of(
                        "listId", num("Target list id."),
                        "listName", str("Target list name (if id not given)."),
                        "itemId", num("Item id to uncheck."),
                        "itemName", str("Item name (if id not given).")),
                List.of());
    }

    private static Map<String, Object> generateShoppingListFromRecipe() {
        return decl(TOOL_GENERATE_SHOPPING_LIST_FROM_RECIPE,
                "Create a new shopping list from an existing saved recipe's ingredients. Use when the user "
                        + "says \"add ingredients for X to my shopping list\", \"make a shopping list for X\", "
                        + "or similar — DO NOT interpret \"ingredients for a recipe\" as pantry items, that "
                        + "is a common mistake. Identify the recipe by recipeId (preferred) or recipeTitle. "
                        + "This creates a NEW list, does not merge into an existing one.",
                Map.of(
                        "recipeId", num("The [recipe:N] id of the recipe."),
                        "recipeTitle", str("Exact recipe title (if id not given).")),
                List.of());
    }

    private static Map<String, Object> createRecipe() {
        return decl(TOOL_CREATE_RECIPE,
                "Save a NEW recipe (title + instructions). Ingredients are NOT part of this call — after the "
                        + "recipe is created, use add_recipe_ingredient separately for each ingredient. "
                        + "NOT for adding items to pantry or shopping list.",
                Map.of(
                        "title", str("Recipe title."),
                        "instructions", str("Step-by-step instructions as free text."),
                        "cookTimeMinutes", num("Optional cook time in minutes."),
                        "tags", Map.of("type", "array",
                                "items", Map.of("type", "string"),
                                "description", "Optional list of tags, e.g. ['italian', 'quick'].")),
                List.of("title", "instructions"));
    }

    private static Map<String, Object> updateRecipe() {
        return decl(TOOL_UPDATE_RECIPE,
                "Update fields on an EXISTING saved recipe (title, instructions, cook time, or tags). "
                        + "Identify by recipeId (preferred) or recipeTitle. Provide only the fields to change; "
                        + "omitted fields keep current values. Use this for \"rename my pasta recipe\", "
                        + "\"change cook time to 30 min\", \"update the instructions on X\", or similar.",
                Map.of(
                        "recipeId", num("The [recipe:N] id of the recipe to update."),
                        "recipeTitle", str("Current recipe title (if id not given)."),
                        "newTitle", str("New title (omit if unchanged)."),
                        "instructions", str("New instructions (omit if unchanged)."),
                        "cookTimeMinutes", num("New cook time in minutes (omit if unchanged)."),
                        "tags", Map.of("type", "array",
                                "items", Map.of("type", "string"),
                                "description", "New tags (omit if unchanged; empty array clears tags).")),
                List.of());
    }

    private static Map<String, Object> deleteRecipe() {
        return decl(TOOL_DELETE_RECIPE,
                "Delete a saved recipe entirely. NOT for pantry items or shopping-list items.",
                Map.of(
                        "recipeId", num("The [recipe:N] id."),
                        "recipeTitle", str("Recipe title (if id not given).")),
                List.of());
    }

    private static Map<String, Object> addRecipeIngredient() {
        return decl(TOOL_ADD_RECIPE_INGREDIENT,
                "Add an ingredient to a saved RECIPE — this modifies the recipe definition itself. "
                        + "NOT for adding to pantry or shopping list. If the user wants the ingredient on their "
                        + "shopping list, use add_shopping_list_item instead.",
                Map.of(
                        "recipeId", num("The [recipe:N] id."),
                        "recipeTitle", str("Recipe title (if id not given)."),
                        "name", str("Ingredient name."),
                        "quantity", num("Quantity."),
                        "unit", str("Unit of measurement.")),
                List.of("name", "quantity", "unit"));
    }

    private static Map<String, Object> removeRecipeIngredient() {
        return decl(TOOL_REMOVE_RECIPE_INGREDIENT,
                "Remove an ingredient from a saved recipe (modifies the recipe definition).",
                Map.of(
                        "recipeId", num("The [recipe:N] id."),
                        "recipeTitle", str("Recipe title (if id not given)."),
                        "ingredientId", num("The [ingredient:N] id."),
                        "ingredientName", str("Ingredient name (if id not given).")),
                List.of());
    }

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
