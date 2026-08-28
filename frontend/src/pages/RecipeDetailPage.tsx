import { useState } from "react";
import type { ReactNode } from "react";
import { Link, Navigate, useNavigate, useParams } from "react-router-dom";
import {
  BookOpen,
  ChevronLeft,
  ClipboardList,
  Clock,
  Pencil,
  Trash2,
  X,
} from "lucide-react";
import { useMutation } from "@tanstack/react-query";
import { Button } from "../components/Button";
import { ErrorState } from "../components/ErrorState";
import { LoadingState } from "../components/LoadingState";
import { EmptyState } from "../components/EmptyState";
import { AddIngredientForm } from "../components/AddIngredientForm";
import { IngredientRow } from "../components/IngredientRow";
import { ApiError } from "../api/client";
import { generateShoppingListFromRecipe } from "../api/recipes";
import {
  useDeleteRecipeMutation,
  useRecipeQuery,
} from "../hooks/useRecipes";
import type { RecipeResponse } from "../types/recipe";

export function RecipeDetailPage(): ReactNode {
  const params = useParams<{ id: string }>();
  const idNum = params.id ? Number(params.id) : NaN;
  const enabled = Number.isFinite(idNum);
  const query = useRecipeQuery(enabled ? idNum : null);

  if (!enabled) return <Navigate to="/recipes" replace />;

  if (query.isPending) {
    return (
      <div className="mx-auto flex max-w-3xl flex-col gap-6">
        <BackLink />
        <LoadingState rows={3} />
      </div>
    );
  }

  const notFound =
    query.isError && query.error instanceof ApiError && query.error.status === 404;

  if (notFound) {
    return (
      <div className="mx-auto flex max-w-3xl flex-col gap-6">
        <BackLink />
        <div className="rounded-lg border border-border-subtle bg-surface-card p-8 dark:border-border-subtle-dark dark:bg-surface-card-dark">
          <EmptyState
            icon={BookOpen}
            title="Recipe not found"
            description="This recipe doesn't exist or was removed."
            action={
              <Link
                to="/recipes"
                className="inline-flex min-h-11 items-center gap-2 rounded-lg bg-primary px-4 text-body-sm font-medium text-white transition-colors duration-150 hover:bg-primary-hover"
              >
                Back to recipes
              </Link>
            }
          />
        </div>
      </div>
    );
  }

  if (query.isError) {
    return (
      <div className="mx-auto flex max-w-3xl flex-col gap-6">
        <BackLink />
        <div className="rounded-lg border border-border-subtle bg-surface-card p-8 dark:border-border-subtle-dark dark:bg-surface-card-dark">
          <ErrorState onRetry={() => void query.refetch()} />
        </div>
      </div>
    );
  }

  return <RecipeDetailContent recipe={query.data} />;
}

function BackLink(): ReactNode {
  return (
    <Link
      to="/recipes"
      className="inline-flex items-center gap-1 text-body-sm text-text-secondary transition-colors duration-150 hover:text-text-primary dark:text-text-secondary-dark dark:hover:text-text-primary-dark"
    >
      <ChevronLeft className="h-4 w-4" aria-hidden />
      Back to recipes
    </Link>
  );
}

interface RecipeDetailContentProps {
  recipe: RecipeResponse;
}

function RecipeDetailContent({ recipe }: RecipeDetailContentProps): ReactNode {
  const navigate = useNavigate();
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [generateError, setGenerateError] = useState<string | null>(null);
  const del = useDeleteRecipeMutation();
  const generate = useMutation({
    mutationFn: (recipeId: number) => generateShoppingListFromRecipe(recipeId),
  });

  const tags = recipe.tags ?? [];

  function handleEditClick(): void {
    navigate(`/recipes/${recipe.id}/edit`);
  }

  async function handleConfirmDelete(): Promise<void> {
    setDeleteError(null);
    try {
      await del.mutateAsync(recipe.id);
      navigate("/recipes");
    } catch (err) {
      setDeleteError(
        err instanceof ApiError ? err.message : "Couldn't delete. Try again.",
      );
      setConfirmingDelete(false);
    }
  }

  async function handleGenerateShoppingList(): Promise<void> {
    setGenerateError(null);
    try {
      const list = await generate.mutateAsync(recipe.id);
      navigate(`/shopping-lists/${list.id}`);
    } catch (err) {
      setGenerateError(
        err instanceof ApiError
          ? err.message
          : "Couldn't generate the shopping list. Try again.",
      );
    }
  }

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <BackLink />

        <header className="flex flex-col gap-3">
          <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
            <div className="min-w-0 flex-1">
              <h1
                className="text-h1 font-semibold text-text-primary dark:text-text-primary-dark"
                data-testid="recipe-title"
              >
                {recipe.title}
              </h1>
              <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-body-sm text-text-secondary dark:text-text-secondary-dark">
                {typeof recipe.cookTimeMinutes === "number" && (
                  <span className="inline-flex items-center gap-1">
                    <Clock className="h-3.5 w-3.5" aria-hidden />
                    {recipe.cookTimeMinutes} min
                  </span>
                )}
              </div>
              {tags.length > 0 && (
                <div className="mt-2 flex flex-wrap gap-1.5">
                  {tags.map((tag) => (
                    <span
                      key={tag}
                      className="inline-flex items-center rounded-md bg-primary/10 px-2 py-0.5 text-caption font-medium text-primary dark:bg-primary/20"
                    >
                      {tag}
                    </span>
                  ))}
                </div>
              )}
            </div>
            <div className="flex flex-wrap gap-2">
              <Button
                variant="secondary"
                onClick={handleEditClick}
                data-testid="edit-recipe-button"
              >
                <Pencil className="h-4 w-4" aria-hidden />
                Edit
              </Button>
              <Button
                variant="secondary"
                onClick={() => setConfirmingDelete(true)}
                className="text-warning hover:bg-warning/10 dark:hover:bg-warning/20"
                data-testid="delete-recipe-button"
              >
                <Trash2 className="h-4 w-4" aria-hidden />
                Delete
              </Button>
            </div>
          </div>

          {confirmingDelete && (
            <div className="flex flex-wrap items-center gap-2 rounded-lg border border-warning/40 bg-warning/5 px-3 py-2 dark:bg-warning/10">
              <span className="flex-1 text-body-sm text-text-primary dark:text-text-primary-dark">
                Delete this recipe?
              </span>
              <Button variant="secondary" onClick={() => setConfirmingDelete(false)}>
                <X className="h-4 w-4" aria-hidden />
                Cancel
              </Button>
              <Button
                className="bg-warning text-white hover:bg-warning/90"
                onClick={handleConfirmDelete}
                loading={del.isPending}
                data-testid="confirm-delete-recipe"
              >
                <Trash2 className="h-4 w-4" aria-hidden />
                Delete
              </Button>
            </div>
          )}
          {deleteError && (
            <p role="alert" className="text-body-sm text-warning">
              {deleteError}
            </p>
          )}
        </header>

        <section
          aria-labelledby="ingredients-heading"
          className="flex flex-col gap-3"
        >
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <h2
              id="ingredients-heading"
              className="text-h3 font-semibold text-text-primary dark:text-text-primary-dark"
            >
              Ingredients
            </h2>
            <Button
              variant="secondary"
              onClick={handleGenerateShoppingList}
              loading={generate.isPending}
              data-testid="generate-shopping-list-button"
            >
              <ClipboardList className="h-4 w-4" aria-hidden />
              Generate shopping list
            </Button>
          </div>
          {generateError && (
            <p role="alert" className="text-body-sm text-warning">
              {generateError}
            </p>
          )}
          {recipe.ingredients.length === 0 ? (
            <p className="text-body-sm text-text-secondary dark:text-text-secondary-dark">
              No ingredients yet. Add one below.
            </p>
          ) : (
            <ul className="flex flex-col gap-2">
              {recipe.ingredients.map((ingredient) => (
                <IngredientRow
                  key={ingredient.id}
                  recipeId={recipe.id}
                  ingredient={ingredient}
                />
              ))}
            </ul>
          )}
          <div className="pt-1">
            <AddIngredientForm recipeId={recipe.id} />
          </div>
        </section>

        <section
          aria-labelledby="instructions-heading"
          className="flex flex-col gap-3"
        >
          <h2
            id="instructions-heading"
            className="text-h3 font-semibold text-text-primary dark:text-text-primary-dark"
          >
            Instructions
          </h2>
          <div
            className="whitespace-pre-line rounded-lg border border-border-subtle bg-white p-4 text-body text-text-primary dark:border-border-subtle-dark dark:bg-surface-card-dark dark:text-text-primary-dark"
            data-testid="recipe-instructions"
          >
            {recipe.instructions}
          </div>
        </section>
    </div>
  );
}
