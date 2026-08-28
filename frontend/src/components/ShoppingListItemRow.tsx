import { useState } from "react";
import type { ReactNode } from "react";
import { Trash2, X } from "lucide-react";
import { Button } from "./Button";
import { ApiError } from "../api/client";
import {
  useDeleteShoppingListItemMutation,
  useToggleShoppingListItemCheckedMutation,
} from "../hooks/useShoppingLists";
import type { ShoppingListItemResponse } from "../types/shoppingList";

interface ShoppingListItemRowProps {
  listId: number;
  item: ShoppingListItemResponse;
}

export function ShoppingListItemRow({
  listId,
  item,
}: ShoppingListItemRowProps): ReactNode {
  const toggle = useToggleShoppingListItemCheckedMutation();
  const del = useDeleteShoppingListItemMutation();
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function handleToggle(): void {
    setError(null);
    toggle.mutate(
      { listId, itemId: item.id, checked: !item.checked },
      {
        onError: (err) => {
          setError(
            err instanceof ApiError ? err.message : "Couldn't update. Try again.",
          );
        },
      },
    );
  }

  async function handleDelete(): Promise<void> {
    setError(null);
    try {
      await del.mutateAsync({ listId, itemId: item.id });
    } catch (err) {
      setError(
        err instanceof ApiError ? err.message : "Couldn't delete. Try again.",
      );
      setConfirmingDelete(false);
    }
  }

  const checkboxId = `sli-check-${item.id}`;
  const nameTone = item.checked
    ? "text-text-secondary line-through dark:text-text-secondary-dark"
    : "text-text-primary dark:text-text-primary-dark";
  const qtyTone = item.checked
    ? "text-text-secondary/70 line-through dark:text-text-secondary-dark/70"
    : "text-text-secondary dark:text-text-secondary-dark";

  return (
    <li
      data-testid={`shopping-list-item-${item.id}`}
      data-checked={item.checked ? "true" : "false"}
      className={
        "flex flex-col gap-2 rounded-lg border border-border-subtle bg-white p-3 md:p-4 " +
        "dark:border-border-subtle-dark dark:bg-surface-card-dark " +
        (item.checked ? "opacity-80" : "")
      }
    >
      <div className="flex items-center gap-3">
        <input
          id={checkboxId}
          type="checkbox"
          checked={item.checked}
          onChange={handleToggle}
          aria-label={item.checked ? `Uncheck ${item.name}` : `Check ${item.name}`}
          className="h-5 w-5 shrink-0 cursor-pointer accent-primary"
          data-testid={`shopping-list-item-checkbox-${item.id}`}
        />
        <label
          htmlFor={checkboxId}
          className="min-w-0 flex-1 cursor-pointer select-none"
        >
          <span
            className={"block truncate text-body font-medium " + nameTone}
            data-testid={`shopping-list-item-name-${item.id}`}
          >
            {item.name}
          </span>
          <span className={"mt-0.5 block text-body-sm " + qtyTone}>
            {formatQuantity(item.quantity)}
            {item.unit ? ` ${item.unit}` : ""}
          </span>
        </label>
        <button
          type="button"
          aria-label={`Delete ${item.name}`}
          onClick={() => setConfirmingDelete(true)}
          className="grid h-11 w-11 shrink-0 place-items-center rounded-lg text-text-secondary transition-colors duration-150 hover:bg-warning/10 hover:text-warning dark:text-text-secondary-dark dark:hover:bg-warning/20"
          data-testid={`shopping-list-item-delete-${item.id}`}
        >
          <Trash2 className="h-4 w-4" aria-hidden />
        </button>
      </div>
      {confirmingDelete && (
        <div className="flex flex-wrap items-center gap-2 rounded-lg border border-warning/40 bg-warning/5 px-3 py-2 dark:bg-warning/10">
          <span className="flex-1 text-body-sm text-text-primary dark:text-text-primary-dark">
            Remove this item?
          </span>
          <Button variant="secondary" onClick={() => setConfirmingDelete(false)}>
            <X className="h-4 w-4" aria-hidden />
            Cancel
          </Button>
          <Button
            className="bg-warning text-white hover:bg-warning/90"
            onClick={handleDelete}
            loading={del.isPending}
            data-testid={`confirm-delete-item-${item.id}`}
          >
            <Trash2 className="h-4 w-4" aria-hidden />
            Remove
          </Button>
        </div>
      )}
      {error && !confirmingDelete && (
        <p role="alert" className="text-body-sm text-warning">
          {error}
        </p>
      )}
    </li>
  );
}

function formatQuantity(value: number): string {
  const rounded = Math.round(value * 1000) / 1000;
  return rounded.toString();
}
