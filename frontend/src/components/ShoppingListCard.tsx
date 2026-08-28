import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { ChevronRight, ClipboardList } from "lucide-react";
import type { ShoppingListSummaryResponse } from "../types/shoppingList";

interface ShoppingListCardProps {
  list: ShoppingListSummaryResponse;
}

export function ShoppingListCard({ list }: ShoppingListCardProps): ReactNode {
  return (
    <li>
      <Link
        to={`/shopping-lists/${list.id}`}
        data-testid={`shopping-list-card-${list.id}`}
        className={
          "group flex items-center gap-3 rounded-lg border border-border-subtle bg-white p-4 md:p-5 " +
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
            {list.name}
          </h3>
          <p className="mt-0.5 text-body-sm text-text-secondary dark:text-text-secondary-dark">
            Updated {formatDate(list.updatedAt)}
          </p>
        </div>
        <ChevronRight
          className="h-4 w-4 shrink-0 text-text-secondary transition-transform duration-150 group-hover:translate-x-0.5 dark:text-text-secondary-dark"
          aria-hidden
        />
      </Link>
    </li>
  );
}

function formatDate(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}
