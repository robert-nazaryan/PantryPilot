import type { PantryItemResponse } from "./pantry";

export interface ChatRequest {
  sessionId: number | null;
  message: string;
}

export type ChatActionType =
  | "CREATE_PANTRY_ITEM"
  | "UPDATE_PANTRY_ITEM"
  | "DELETE_PANTRY_ITEM"
  | "CONSUME_PANTRY_ITEM"
  | "CREATE_SHOPPING_LIST"
  | "ADD_SHOPPING_LIST_ITEM"
  | "REMOVE_SHOPPING_LIST_ITEM"
  | "CHECK_SHOPPING_LIST_ITEM"
  | "UNCHECK_SHOPPING_LIST_ITEM"
  | "GENERATE_SHOPPING_LIST_FROM_RECIPE"
  | "CREATE_RECIPE"
  | "DELETE_RECIPE"
  | "ADD_RECIPE_INGREDIENT"
  | "REMOVE_RECIPE_INGREDIENT"
  | "BULK_ACTION";

export type ChatActionStatus = "PENDING" | "CONFIRMED";

export interface CreatePantryItemPayload {
  name: string;
  quantity: number;
  unit: string;
  category?: string | null;
  expiryDate?: string | null;
}

export interface UpdatePantryItemPayload {
  itemId: number;
  name: string;
  quantity: number;
  unit: string;
  category?: string | null;
  expiryDate?: string | null;
}

export interface DeletePantryItemPayload {
  itemId: number;
  name: string;
}

export interface ConsumePantryItemPayload {
  itemId: number;
  name: string;
  quantity: number;
  unit: string;
  availableQuantity: number;
}

export interface CreateShoppingListPayload {
  name?: string | null;
}

export interface AddShoppingListItemPayload {
  listId: number;
  listName: string;
  name: string;
  quantity?: number | null;
  unit?: string | null;
}

export interface RemoveShoppingListItemPayload {
  listId: number;
  listName: string;
  itemId: number;
  itemName: string;
}

export interface SetShoppingListItemCheckedPayload {
  listId: number;
  listName: string;
  itemId: number;
  itemName: string;
  checked: boolean;
}

export interface GenerateShoppingListFromRecipePayload {
  recipeId: number;
  recipeTitle: string;
}

export interface CreateRecipePayload {
  title: string;
  instructions: string;
  cookTimeMinutes?: number | null;
  tags?: string[] | null;
}

export interface DeleteRecipePayload {
  recipeId: number;
  recipeTitle: string;
}

export interface AddRecipeIngredientPayload {
  recipeId: number;
  recipeTitle: string;
  name: string;
  quantity: number;
  unit: string;
}

export interface RemoveRecipeIngredientPayload {
  recipeId: number;
  recipeTitle: string;
  ingredientId: number;
  ingredientName: string;
}

export interface BulkActionPayload {
  subActionType: ChatActionType;
  targets: unknown[];
  summary: string;
}

interface ProposedActionBase {
  actionId: number;
  status: ChatActionStatus;
}

export type ProposedAction =
  | (ProposedActionBase & { type: "CREATE_PANTRY_ITEM"; payload: CreatePantryItemPayload })
  | (ProposedActionBase & { type: "UPDATE_PANTRY_ITEM"; payload: UpdatePantryItemPayload })
  | (ProposedActionBase & { type: "DELETE_PANTRY_ITEM"; payload: DeletePantryItemPayload })
  | (ProposedActionBase & { type: "CONSUME_PANTRY_ITEM"; payload: ConsumePantryItemPayload })
  | (ProposedActionBase & { type: "CREATE_SHOPPING_LIST"; payload: CreateShoppingListPayload })
  | (ProposedActionBase & { type: "ADD_SHOPPING_LIST_ITEM"; payload: AddShoppingListItemPayload })
  | (ProposedActionBase & { type: "REMOVE_SHOPPING_LIST_ITEM"; payload: RemoveShoppingListItemPayload })
  | (ProposedActionBase & { type: "CHECK_SHOPPING_LIST_ITEM"; payload: SetShoppingListItemCheckedPayload })
  | (ProposedActionBase & { type: "UNCHECK_SHOPPING_LIST_ITEM"; payload: SetShoppingListItemCheckedPayload })
  | (ProposedActionBase & { type: "GENERATE_SHOPPING_LIST_FROM_RECIPE"; payload: GenerateShoppingListFromRecipePayload })
  | (ProposedActionBase & { type: "CREATE_RECIPE"; payload: CreateRecipePayload })
  | (ProposedActionBase & { type: "DELETE_RECIPE"; payload: DeleteRecipePayload })
  | (ProposedActionBase & { type: "ADD_RECIPE_INGREDIENT"; payload: AddRecipeIngredientPayload })
  | (ProposedActionBase & { type: "REMOVE_RECIPE_INGREDIENT"; payload: RemoveRecipeIngredientPayload })
  | (ProposedActionBase & { type: "BULK_ACTION"; payload: BulkActionPayload });

export interface ChatResponse {
  sessionId: number;
  reply: string;
  proposedAction?: ProposedAction | null;
}

export interface BulkActionFailure {
  target: unknown;
  reason: string;
}

export interface BulkActionResult {
  succeeded: number;
  failed: number;
  failures: BulkActionFailure[];
}

export interface ConfirmActionResponse {
  actionType: ChatActionType;
  result: PantryItemResponse | unknown | null;
  bulkResult?: BulkActionResult | null;
}
