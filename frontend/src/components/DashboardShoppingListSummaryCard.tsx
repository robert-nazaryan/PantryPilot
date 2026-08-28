import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { ChevronRight, ClipboardList } from "lucide-react";

interface DashboardShoppingListSummaryCardProps {
  listId: number;
  name: string;
  total: number;
  checked: number;
}

export function DashboardShoppingListSummaryCard({
  listId,
  name,
  total,
  checked,
}: DashboardShoppingListSummaryCardProps): ReactNode {
  const progressPct = total > 0 ? Math.round((checked / total) * 100) : 0;

  return (
    <li>
      <Link
        to={`/shopping-lists/${listId}`}
        data-testid={`dashboard-shopping-list-card-${listId}`}
        className={
          "group flex items-center gap-3 rounded-lg border border-border-subtle bg-white p-4 " +
          "transition-colors duration-150 " +
          "hover:border-primary/40 hover:bg-surface-card " +
          "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary " +
          "dark:border-border-subtle-dark dark:bg-surface-card-dark " +
          "dark:hover:border-primary/40 dark:hover:bg-surface-elevated-dark"
        }
      >
        <span className="grid h-10 w-10 shrink-0 place-items-center rounded-lg bg-primary/10 text-primary dark:bg-primary/20">
          <ClipboardList className="h-5 w-5" aria-hidden />
        </span>
        <div className="min-w-0 flex-1">
          <h3 className="truncate text-body font-semibold text-text-primary dark:text-text-primary-dark">
            {name}
          </h3>
          {total === 0 ? (
            <p className="mt-1 text-body-sm text-text-secondary dark:text-text-secondary-dark">
              No items yet
            </p>
          ) : (
            <div className="mt-1.5 flex flex-col gap-1">
              <p className="text-body-sm text-text-secondary dark:text-text-secondary-dark">
                {checked} of {total} checked
              </p>
              <div
                role="progressbar"
                aria-valuemin={0}
                aria-valuemax={total}
                aria-valuenow={checked}
                className="h-1 w-full overflow-hidden rounded-full bg-border-subtle dark:bg-border-subtle-dark"
              >
                <div
                  className="h-full rounded-full bg-primary transition-all duration-200"
                  style={{ width: `${progressPct}%` }}
                />
              </div>
            </div>
          )}
        </div>
        <ChevronRight
          className="h-4 w-4 shrink-0 text-text-secondary transition-transform duration-150 group-hover:translate-x-0.5 dark:text-text-secondary-dark"
          aria-hidden
        />
      </Link>
    </li>
  );
}
