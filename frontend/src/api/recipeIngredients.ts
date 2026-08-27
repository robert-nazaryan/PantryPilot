import { apiFetch } from "./client";
import type {
  CreateRecipeIngredientRequest,
  RecipeIngredientResponse,
  UpdateRecipeIngredientRequest,
} from "../types/recipe";

export function addRecipeIngredient(
  recipeId: number,
  body: CreateRecipeIngredientRequest,
): Promise<RecipeIngredientResponse> {
  return apiFetch<RecipeIngredientResponse>(
    `/api/recipes/${recipeId}/ingredients`,
    { method: "POST", body },
  );
}

export function updateRecipeIngredient(
  recipeId: number,
  ingredientId: number,
  body: UpdateRecipeIngredientRequest,
): Promise<RecipeIngredientResponse> {
  return apiFetch<RecipeIngredientResponse>(
    `/api/recipes/${recipeId}/ingredients/${ingredientId}`,
    { method: "PUT", body },
  );
}

export function deleteRecipeIngredient(
  recipeId: number,
  ingredientId: number,
): Promise<void> {
  return apiFetch<void>(
    `/api/recipes/${recipeId}/ingredients/${ingredientId}`,
    { method: "DELETE" },
  );
}
