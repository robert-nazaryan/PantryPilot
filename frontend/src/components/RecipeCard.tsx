import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { ChevronRight, Clock } from "lucide-react";
import type { RecipeSummaryResponse } from "../types/recipe";

interface RecipeCardProps {
  recipe: RecipeSummaryResponse;
}

export function RecipeCard({ recipe }: RecipeCardProps): ReactNode {
  const tags = recipe.tags ?? [];
  return (
    <li>
      <Link
        to={`/recipes/${recipe.id}`}
        data-testid={`recipe-card-${recipe.id}`}
        className={
          "group flex items-center gap-3 rounded-lg border border-border-subtle bg-white p-4 md:p-5 " +
          "transition-colors duration-150 " +
          "hover:border-primary/40 hover:bg-surface-card " +
          "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary " +
          "dark:border-border-subtle-dark dark:bg-surface-card-dark " +
          "dark:hover:border-primary/40 dark:hover:bg-surface-elevated-dark"
        }
      >
        <div className="min-w-0 flex-1">
          <h3 className="text-body font-semibold text-text-primary dark:text-text-primary-dark">
            {recipe.title}
          </h3>
          <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-body-sm text-text-secondary dark:text-text-secondary-dark">
            {typeof recipe.cookTimeMinutes === "number" && (
              <span className="inline-flex items-center gap-1">
                <Clock className="h-3.5 w-3.5" aria-hidden />
                {recipe.cookTimeMinutes} min
              </span>
            )}
            {tags.length > 0 && (
              <div className="flex flex-wrap items-center gap-1">
                {tags.slice(0, 4).map((tag) => (
                  <span
                    key={tag}
                    className="inline-flex items-center rounded-md bg-primary/10 px-2 py-0.5 text-caption font-medium text-primary dark:bg-primary/20"
                  >
                    {tag}
                  </span>
                ))}
                {tags.length > 4 && (
                  <span className="text-caption text-text-secondary dark:text-text-secondary-dark">
                    +{tags.length - 4}
                  </span>
                )}
              </div>
            )}
          </div>
        </div>
        <ChevronRight
          className="h-4 w-4 shrink-0 text-text-secondary transition-transform duration-150 group-hover:translate-x-0.5 dark:text-text-secondary-dark"
          aria-hidden
        />
      </Link>
    </li>
  );
}
