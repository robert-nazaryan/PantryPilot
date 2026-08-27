import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { UseQueryResult } from "@tanstack/react-query";
import * as recipesApi from "../api/recipes";
import * as ingredientsApi from "../api/recipeIngredients";
import type { PageResponse } from "../types/page";
import type {
  CreateRecipeIngredientRequest,
  CreateRecipeRequest,
  RecipeIngredientResponse,
  RecipeResponse,
  RecipeSummaryResponse,
  UpdateRecipeIngredientRequest,
  UpdateRecipeRequest,
} from "../types/recipe";

export const RECIPES_QUERY_KEY = ["recipes"] as const;
export const DEFAULT_PAGE_SIZE = 20;

interface UseRecipesQueryOptions {
  page: number;
  size?: number;
}

export function useRecipesQuery({
  page,
  size = DEFAULT_PAGE_SIZE,
}: UseRecipesQueryOptions): UseQueryResult<PageResponse<RecipeSummaryResponse>> {
  return useQuery({
    queryKey: [...RECIPES_QUERY_KEY, "list", { page, size }],
    queryFn: () => recipesApi.listRecipes({ page, size }),
    placeholderData: (previous) => previous,
  });
}

export function useRecipeQuery(id: number | null): UseQueryResult<RecipeResponse> {
  return useQuery({
    queryKey: [...RECIPES_QUERY_KEY, "detail", id],
    queryFn: () => recipesApi.getRecipe(id as number),
    enabled: id !== null,
    retry: (failureCount, error) => {
      const status = (error as { status?: number } | null)?.status;
      if (status === 404) return false;
      return failureCount < 3;
    },
  });
}

function useInvalidateRecipeLists() {
  const qc = useQueryClient();
  return () => qc.invalidateQueries({ queryKey: [...RECIPES_QUERY_KEY, "list"] });
}

function useInvalidateRecipeDetail() {
  const qc = useQueryClient();
  return (id: number) =>
    qc.invalidateQueries({ queryKey: [...RECIPES_QUERY_KEY, "detail", id] });
}

export function useCreateRecipeMutation() {
  const invalidateLists = useInvalidateRecipeLists();
  return useMutation({
    mutationFn: (body: CreateRecipeRequest) => recipesApi.createRecipe(body),
    onSuccess: () => invalidateLists(),
  });
}

export function useUpdateRecipeMutation() {
  const invalidateLists = useInvalidateRecipeLists();
  const invalidateDetail = useInvalidateRecipeDetail();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: UpdateRecipeRequest }) =>
      recipesApi.updateRecipe(id, body),
    onSuccess: (data) => {
      invalidateLists();
      invalidateDetail(data.id);
    },
  });
}

export function useDeleteRecipeMutation() {
  const qc = useQueryClient();
  const invalidateLists = useInvalidateRecipeLists();
  return useMutation({
    mutationFn: (id: number) => recipesApi.deleteRecipe(id),
    onSuccess: (_data, id) => {
      invalidateLists();
      qc.removeQueries({ queryKey: [...RECIPES_QUERY_KEY, "detail", id] });
    },
  });
}

export function useAddIngredientMutation() {
  const invalidateDetail = useInvalidateRecipeDetail();
  return useMutation({
    mutationFn: ({
      recipeId,
      body,
    }: {
      recipeId: number;
      body: CreateRecipeIngredientRequest;
    }): Promise<RecipeIngredientResponse> =>
      ingredientsApi.addRecipeIngredient(recipeId, body),
    onSuccess: (_data, variables) => invalidateDetail(variables.recipeId),
  });
}

export function useUpdateIngredientMutation() {
  const invalidateDetail = useInvalidateRecipeDetail();
  return useMutation({
    mutationFn: ({
      recipeId,
      ingredientId,
      body,
    }: {
      recipeId: number;
      ingredientId: number;
      body: UpdateRecipeIngredientRequest;
    }): Promise<RecipeIngredientResponse> =>
      ingredientsApi.updateRecipeIngredient(recipeId, ingredientId, body),
    onSuccess: (_data, variables) => invalidateDetail(variables.recipeId),
  });
}

export function useDeleteIngredientMutation() {
  const invalidateDetail = useInvalidateRecipeDetail();
  return useMutation({
    mutationFn: ({
      recipeId,
      ingredientId,
    }: {
      recipeId: number;
      ingredientId: number;
    }) => ingredientsApi.deleteRecipeIngredient(recipeId, ingredientId),
    onSuccess: (_data, variables) => invalidateDetail(variables.recipeId),
  });
}
