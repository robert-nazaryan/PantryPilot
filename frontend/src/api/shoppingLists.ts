import { apiFetch } from "./client";
import type { PageResponse } from "../types/page";
import type {
  CreateShoppingListRequest,
  ShoppingListResponse,
  ShoppingListSummaryResponse,
  UpdateShoppingListRequest,
} from "../types/shoppingList";

export interface ListShoppingListsParams {
  page?: number;
  size?: number;
}

export function listShoppingLists(
  params: ListShoppingListsParams = {},
): Promise<PageResponse<ShoppingListSummaryResponse>> {
  const search = new URLSearchParams();
  if (params.page !== undefined) search.set("page", String(params.page));
  if (params.size !== undefined) search.set("size", String(params.size));
  const query = search.toString();
  return apiFetch<PageResponse<ShoppingListSummaryResponse>>(
    `/api/shopping-lists${query ? `?${query}` : ""}`,
  );
}

export function getShoppingList(id: number): Promise<ShoppingListResponse> {
  return apiFetch<ShoppingListResponse>(`/api/shopping-lists/${id}`);
}

export function createShoppingList(
  body: CreateShoppingListRequest,
): Promise<ShoppingListResponse> {
  return apiFetch<ShoppingListResponse>("/api/shopping-lists", {
    method: "POST",
    body,
  });
}

export function updateShoppingList(
  id: number,
  body: UpdateShoppingListRequest,
): Promise<ShoppingListResponse> {
  return apiFetch<ShoppingListResponse>(`/api/shopping-lists/${id}`, {
    method: "PUT",
    body,
  });
}

export function deleteShoppingList(id: number): Promise<void> {
  return apiFetch<void>(`/api/shopping-lists/${id}`, { method: "DELETE" });
}
