import { useMemo, useState } from "react";
import type { ReactNode } from "react";
import { Link, Navigate, useNavigate, useParams } from "react-router-dom";
import { ChevronLeft, ClipboardList, Trash2, X } from "lucide-react";
import { Button } from "../components/Button";
import { EmptyState } from "../components/EmptyState";
import { ErrorState } from "../components/ErrorState";
import { LoadingState } from "../components/LoadingState";
import { InlineShoppingListName } from "../components/InlineShoppingListName";
import { AddShoppingListItemRow } from "../components/AddShoppingListItemRow";
import { ShoppingListItemRow } from "../components/ShoppingListItemRow";
import { ApiError } from "../api/client";
import {
  useDeleteShoppingListMutation,
  useShoppingListQuery,
} from "../hooks/useShoppingLists";
import type { ShoppingListResponse } from "../types/shoppingList";

export function ShoppingListDetailPage(): ReactNode {
  const params = useParams<{ id: string }>();
  const idNum = params.id ? Number(params.id) : NaN;
  const enabled = Number.isFinite(idNum);
  const query = useShoppingListQuery(enabled ? idNum : null);

  if (!enabled) return <Navigate to="/shopping-lists" replace />;

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
            icon={ClipboardList}
            title="Shopping list not found"
            description="This list doesn't exist or was removed."
            action={
              <Link
                to="/shopping-lists"
                className="inline-flex min-h-11 items-center gap-2 rounded-lg bg-primary px-4 text-body-sm font-medium text-white transition-colors duration-150 hover:bg-primary-hover"
              >
                Back to shopping lists
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

  return <ShoppingListDetailContent list={query.data} />;
}

function BackLink(): ReactNode {
  return (
    <Link
      to="/shopping-lists"
      className="inline-flex items-center gap-1 text-body-sm text-text-secondary transition-colors duration-150 hover:text-text-primary dark:text-text-secondary-dark dark:hover:text-text-primary-dark"
    >
      <ChevronLeft className="h-4 w-4" aria-hidden />
      Back to shopping lists
    </Link>
  );
}

interface ShoppingListDetailContentProps {
  list: ShoppingListResponse;
}

function ShoppingListDetailContent({
  list,
}: ShoppingListDetailContentProps): ReactNode {
  const navigate = useNavigate();
  const del = useDeleteShoppingListMutation();
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const { unchecked, checked } = useMemo(() => {
    const u: typeof list.items = [];
    const c: typeof list.items = [];
    for (const item of list.items) {
      (item.checked ? c : u).push(item);
    }
    return { unchecked: u, checked: c };
  }, [list.items]);

  const total = list.items.length;
  const checkedCount = checked.length;
  const progressPct = total > 0 ? Math.round((checkedCount / total) * 100) : 0;

  async function handleConfirmDelete(): Promise<void> {
    setDeleteError(null);
    try {
      await del.mutateAsync(list.id);
      navigate("/shopping-lists");
    } catch (err) {
      setDeleteError(
        err instanceof ApiError ? err.message : "Couldn't delete. Try again.",
      );
      setConfirmingDelete(false);
    }
  }

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <BackLink />

      <header className="flex flex-col gap-3">
        <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
          <div className="min-w-0 flex-1">
            <InlineShoppingListName
              listId={list.id}
              name={list.name}
              active={list.active}
            />
            {total > 0 && (
              <div
                className="mt-3 flex flex-col gap-1.5"
                data-testid="shopping-list-progress"
              >
                <div className="flex items-center justify-between text-body-sm text-text-secondary dark:text-text-secondary-dark">
                  <span>
                    {checkedCount} of {total} checked
                  </span>
                  <span aria-hidden>{progressPct}%</span>
                </div>
                <div
                  role="progressbar"
                  aria-valuemin={0}
                  aria-valuemax={total}
                  aria-valuenow={checkedCount}
                  className="h-1.5 w-full overflow-hidden rounded-full bg-border-subtle dark:bg-border-subtle-dark"
                >
                  <div
                    className="h-full rounded-full bg-primary transition-all duration-200"
                    style={{ width: `${progressPct}%` }}
                  />
                </div>
              </div>
            )}
          </div>
          <div className="flex flex-wrap gap-2">
            <Button
              variant="secondary"
              onClick={() => setConfirmingDelete(true)}
              className="text-warning hover:bg-warning/10 dark:hover:bg-warning/20"
              data-testid="delete-shopping-list-button"
            >
              <Trash2 className="h-4 w-4" aria-hidden />
              Delete list
            </Button>
          </div>
        </div>

        {confirmingDelete && (
          <div className="flex flex-wrap items-center gap-2 rounded-lg border border-warning/40 bg-warning/5 px-3 py-2 dark:bg-warning/10">
            <span className="flex-1 text-body-sm text-text-primary dark:text-text-primary-dark">
              Delete this shopping list?
            </span>
            <Button variant="secondary" onClick={() => setConfirmingDelete(false)}>
              <X className="h-4 w-4" aria-hidden />
              Cancel
            </Button>
            <Button
              className="bg-warning text-white hover:bg-warning/90"
              onClick={handleConfirmDelete}
              loading={del.isPending}
              data-testid="confirm-delete-shopping-list"
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

      <section aria-label="Add item" className="flex flex-col gap-3">
        <AddShoppingListItemRow listId={list.id} />
      </section>

      {total === 0 ? (
        <div className="rounded-lg border border-border-subtle bg-surface-card p-8 dark:border-border-subtle-dark dark:bg-surface-card-dark">
          <EmptyState
            icon={ClipboardList}
            title="This list is empty"
            description="Add items above, or generate a list from one of your recipes."
          />
        </div>
      ) : (
        <div className="flex flex-col gap-6">
          {unchecked.length > 0 && (
            <section aria-labelledby="to-buy-heading" className="flex flex-col gap-3">
              <h2
                id="to-buy-heading"
                className="text-h3 font-semibold text-text-primary dark:text-text-primary-dark"
              >
                To buy
              </h2>
              <ul className="flex flex-col gap-2">
                {unchecked.map((item) => (
                  <ShoppingListItemRow
                    key={item.id}
                    listId={list.id}
                    item={item}
                  />
                ))}
              </ul>
            </section>
          )}

          {checked.length > 0 && (
            <section aria-labelledby="checked-heading" className="flex flex-col gap-3">
              <h2
                id="checked-heading"
                className="text-h3 font-semibold text-text-secondary dark:text-text-secondary-dark"
              >
                Checked ({checked.length})
              </h2>
              <ul className="flex flex-col gap-2">
                {checked.map((item) => (
                  <ShoppingListItemRow
                    key={item.id}
                    listId={list.id}
                    item={item}
                  />
                ))}
              </ul>
            </section>
          )}
        </div>
      )}
    </div>
  );
}
