export interface RecipeIngredientResponse {
  id: number;
  name: string;
  quantity: number;
  unit: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RecipeResponse {
  id: number;
  title: string;
  instructions: string;
  cookTimeMinutes: number | null;
  tags: string[] | null;
  createdAt: string;
  updatedAt: string;
  ingredients: RecipeIngredientResponse[];
}

export interface RecipeSummaryResponse {
  id: number;
  title: string;
  cookTimeMinutes: number | null;
  tags: string[] | null;
  createdAt: string;
}

export interface CreateRecipeRequest {
  title: string;
  instructions: string;
  cookTimeMinutes?: number | null;
  tags?: string[] | null;
}

export interface UpdateRecipeRequest {
  title: string;
  instructions: string;
  cookTimeMinutes?: number | null;
  tags?: string[] | null;
}

export interface CreateRecipeIngredientRequest {
  name: string;
  quantity: number;
  unit?: string | null;
}

export interface UpdateRecipeIngredientRequest {
  name: string;
  quantity: number;
  unit?: string | null;
}
