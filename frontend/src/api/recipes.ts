import { apiFetch } from "./client";
import type { PageResponse } from "../types/page";
import type {
  CreateRecipeRequest,
  RecipeResponse,
  RecipeSummaryResponse,
  UpdateRecipeRequest,
} from "../types/recipe";
import type { ShoppingListResponse } from "../types/shoppingList";

export interface ListRecipesParams {
  page?: number;
  size?: number;
}

export function listRecipes(
  params: ListRecipesParams = {},
): Promise<PageResponse<RecipeSummaryResponse>> {
  const search = new URLSearchParams();
  if (params.page !== undefined) search.set("page", String(params.page));
  if (params.size !== undefined) search.set("size", String(params.size));
  const query = search.toString();
  return apiFetch<PageResponse<RecipeSummaryResponse>>(
    `/api/recipes${query ? `?${query}` : ""}`,
  );
}

export function getRecipe(id: number): Promise<RecipeResponse> {
  return apiFetch<RecipeResponse>(`/api/recipes/${id}`);
}

export function createRecipe(body: CreateRecipeRequest): Promise<RecipeResponse> {
  return apiFetch<RecipeResponse>("/api/recipes", { method: "POST", body });
}

export function updateRecipe(
  id: number,
  body: UpdateRecipeRequest,
): Promise<RecipeResponse> {
  return apiFetch<RecipeResponse>(`/api/recipes/${id}`, { method: "PUT", body });
}

export function deleteRecipe(id: number): Promise<void> {
  return apiFetch<void>(`/api/recipes/${id}`, { method: "DELETE" });
}

export function generateShoppingListFromRecipe(
  recipeId: number,
): Promise<ShoppingListResponse> {
  return apiFetch<ShoppingListResponse>(
    `/api/recipes/${recipeId}/generate-shopping-list`,
    { method: "POST" },
  );
}
