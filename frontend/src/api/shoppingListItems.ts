import { apiFetch } from "./client";
import type {
  CreateShoppingListItemRequest,
  ShoppingListItemResponse,
  ToggleShoppingListItemCheckedRequest,
  UpdateShoppingListItemRequest,
} from "../types/shoppingList";

export function addShoppingListItem(
  listId: number,
  body: CreateShoppingListItemRequest,
): Promise<ShoppingListItemResponse> {
  return apiFetch<ShoppingListItemResponse>(
    `/api/shopping-lists/${listId}/items`,
    { method: "POST", body },
  );
}

export function updateShoppingListItem(
  listId: number,
  itemId: number,
  body: UpdateShoppingListItemRequest,
): Promise<ShoppingListItemResponse> {
  return apiFetch<ShoppingListItemResponse>(
    `/api/shopping-lists/${listId}/items/${itemId}`,
    { method: "PUT", body },
  );
}

export function setShoppingListItemChecked(
  listId: number,
  itemId: number,
  body: ToggleShoppingListItemCheckedRequest,
): Promise<ShoppingListItemResponse> {
  return apiFetch<ShoppingListItemResponse>(
    `/api/shopping-lists/${listId}/items/${itemId}/check`,
    { method: "PATCH", body },
  );
}

export function deleteShoppingListItem(
  listId: number,
  itemId: number,
): Promise<void> {
  return apiFetch<void>(`/api/shopping-lists/${listId}/items/${itemId}`, {
    method: "DELETE",
  });
}
