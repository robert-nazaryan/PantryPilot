import { useState } from "react";
import type { ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { BookOpen, Plus } from "lucide-react";
import { Button } from "../components/Button";
import { EmptyState } from "../components/EmptyState";
import { ErrorState } from "../components/ErrorState";
import { LoadingState } from "../components/LoadingState";
import { Pagination } from "../components/Pagination";
import { RecipeCard } from "../components/RecipeCard";
import { useRecipesQuery } from "../hooks/useRecipes";

// Recipes intentionally deviates from Pantry Items' dual-mode form pattern:
// the recipe form (title + multi-line instructions + tags) is too tall to fit
// comfortably in a modal, and creating a recipe naturally hands off to the
// detail page for ingredient entry — so a full-page transition on every
// viewport reads better than opening a dialog on desktop.
export function RecipesPage(): ReactNode {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);

  const query = useRecipesQuery({ page });

  function handleAddClick(): void {
    navigate("/recipes/new");
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-h1 font-semibold text-text-primary dark:text-text-primary-dark">
            Recipes
          </h1>
          <p className="mt-1 text-body text-text-secondary dark:text-text-secondary-dark">
            Save what you cook, and plan what&rsquo;s next.
          </p>
        </div>
        <Button
          onClick={handleAddClick}
          className="self-start md:self-auto"
          data-testid="add-recipe-button"
        >
          <Plus className="h-4 w-4" aria-hidden />
          Add recipe
        </Button>
      </div>

      <RecipesContent
        state={query}
        onAdd={handleAddClick}
        page={page}
        onPageChange={setPage}
      />
    </div>
  );
}

interface RecipesContentProps {
  state: ReturnType<typeof useRecipesQuery>;
  onAdd: () => void;
  page: number;
  onPageChange: (page: number) => void;
}

function RecipesContent({
  state,
  onAdd,
  page,
  onPageChange,
}: RecipesContentProps): ReactNode {
  if (state.isPending) {
    return <LoadingState rows={4} />;
  }
  if (state.isError) {
    return (
      <div className="rounded-lg border border-border-subtle bg-surface-card p-8 dark:border-border-subtle-dark dark:bg-surface-card-dark">
        <ErrorState onRetry={() => void state.refetch()} />
      </div>
    );
  }
  const data = state.data;
  if (data.totalElements === 0) {
    return (
      <div className="rounded-lg border border-border-subtle bg-surface-card p-8 dark:border-border-subtle-dark dark:bg-surface-card-dark">
        <EmptyState
          icon={BookOpen}
          title="No recipes yet"
          description="Save your favorite meals to plan them, cook them, and generate shopping lists."
          action={
            <Button onClick={onAdd}>
              <Plus className="h-4 w-4" aria-hidden />
              Create your first recipe
            </Button>
          }
        />
      </div>
    );
  }
  return (
    <div className="flex flex-col gap-4">
      <ul className="flex flex-col gap-3">
        {data.content.map((recipe) => (
          <RecipeCard key={recipe.id} recipe={recipe} />
        ))}
      </ul>
      <Pagination page={page} totalPages={data.totalPages} onChange={onPageChange} />
    </div>
  );
}
