import { apiFetch } from "./client";
import type { PageResponse } from "../types/page";
import type {
  ConsumeQuantityRequest,
  CreatePantryItemRequest,
  PantryItemResponse,
  UpdatePantryItemRequest,
} from "../types/pantry";

export interface ListPantryItemsParams {
  page?: number;
  size?: number;
}

export function listPantryItems(
  params: ListPantryItemsParams = {},
): Promise<PageResponse<PantryItemResponse>> {
  const search = new URLSearchParams();
  if (params.page !== undefined) search.set("page", String(params.page));
  if (params.size !== undefined) search.set("size", String(params.size));
  const query = search.toString();
  return apiFetch<PageResponse<PantryItemResponse>>(
    `/api/pantry-items${query ? `?${query}` : ""}`,
  );
}

export function listExpiringPantryItems(days?: number): Promise<PantryItemResponse[]> {
  const search = new URLSearchParams();
  if (days !== undefined) search.set("days", String(days));
  const query = search.toString();
  return apiFetch<PantryItemResponse[]>(
    `/api/pantry-items/expiring${query ? `?${query}` : ""}`,
  );
}

export function getPantryItem(id: number): Promise<PantryItemResponse> {
  return apiFetch<PantryItemResponse>(`/api/pantry-items/${id}`);
}

export function createPantryItem(
  body: CreatePantryItemRequest,
): Promise<PantryItemResponse> {
  return apiFetch<PantryItemResponse>("/api/pantry-items", { method: "POST", body });
}

export function updatePantryItem(
  id: number,
  body: UpdatePantryItemRequest,
): Promise<PantryItemResponse> {
  return apiFetch<PantryItemResponse>(`/api/pantry-items/${id}`, { method: "PUT", body });
}

export function consumePantryItem(
  id: number,
  body: ConsumeQuantityRequest,
): Promise<PantryItemResponse> {
  return apiFetch<PantryItemResponse>(`/api/pantry-items/${id}/consume`, {
    method: "PATCH",
    body,
  });
}

export function deletePantryItem(id: number): Promise<void> {
  return apiFetch<void>(`/api/pantry-items/${id}`, { method: "DELETE" });
}
