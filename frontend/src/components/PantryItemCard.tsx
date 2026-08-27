import { useState } from "react";
import type { ReactNode } from "react";
import { CalendarClock, CircleAlert, Pencil, Trash2 } from "lucide-react";
import { Button } from "./Button";
import type { PantryItemResponse } from "../types/pantry";
import { ApiError } from "../api/client";
import { useConsumePantryItemMutation, useDeletePantryItemMutation } from "../hooks/usePantryItems";

interface PantryItemCardProps {
  item: PantryItemResponse;
  onEdit: (item: PantryItemResponse) => void;
}

const EXPIRING_SOON_DAYS = 7;
const QUICK_DECREMENTS: number[] = [1, 0.5];

export function PantryItemCard({ item, onEdit }: PantryItemCardProps): ReactNode {
  const consume = useConsumePantryItemMutation();
  const del = useDeletePantryItemMutation();
  const [consumeError, setConsumeError] = useState<string | null>(null);
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  const isEmpty = item.quantity === 0;
  const expiryStatus = getExpiryStatus(item.expiryDate);

  async function handleConsume(amount: number) {
    setConsumeError(null);
    try {
      await consume.mutateAsync({ id: item.id, body: { quantity: amount } });
    } catch (err) {
      if (err instanceof ApiError && err.code === "insufficient_quantity") {
        setConsumeError(`You only have ${formatQuantity(item.quantity)} ${item.unit}.`);
      } else {
        setConsumeError("Couldn't update. Try again.");
      }
    }
  }

  async function handleDelete() {
    try {
      await del.mutateAsync(item.id);
    } catch {
      setConsumeError("Couldn't delete. Try again.");
      setConfirmingDelete(false);
    }
  }

  return (
    <li
      data-testid={`pantry-item-${item.id}`}
      className={
        "flex flex-col gap-3 rounded-lg border border-border-subtle bg-white p-4 md:p-5 " +
        "transition-colors duration-150 " +
        "dark:border-border-subtle-dark dark:bg-surface-card-dark " +
        (isEmpty ? "opacity-70" : "")
      }
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <h3 className="text-body font-semibold text-text-primary dark:text-text-primary-dark">
            {item.name}
          </h3>
          <p className="mt-1 text-body-sm text-text-secondary dark:text-text-secondary-dark">
            <span className={isEmpty ? "text-warning" : ""}>
              {formatQuantity(item.quantity)} {item.unit}
            </span>
            {item.category ? (
              <>
                <span className="mx-2 text-border-subtle dark:text-border-subtle-dark">·</span>
                {item.category}
              </>
            ) : null}
          </p>
          {expiryStatus && (
            <p
              className={
                "mt-1 flex items-center gap-1.5 text-body-sm " +
                (expiryStatus.tone === "warning"
                  ? "text-warning"
                  : "text-text-secondary dark:text-text-secondary-dark")
              }
            >
              {expiryStatus.tone === "warning" ? (
                <CircleAlert className="h-3.5 w-3.5" aria-hidden />
              ) : (
                <CalendarClock className="h-3.5 w-3.5" aria-hidden />
              )}
              {expiryStatus.label}
            </p>
          )}
        </div>

        <div className="flex shrink-0 items-center gap-1">
          <button
            type="button"
            aria-label={`Edit ${item.name}`}
            onClick={() => onEdit(item)}
            className="grid h-11 w-11 place-items-center rounded-lg text-text-secondary transition-colors duration-150 hover:bg-surface-card hover:text-text-primary dark:text-text-secondary-dark dark:hover:bg-surface-elevated-dark dark:hover:text-text-primary-dark"
          >
            <Pencil className="h-4 w-4" aria-hidden />
          </button>
          <button
            type="button"
            aria-label={`Delete ${item.name}`}
            onClick={() => setConfirmingDelete(true)}
            className="grid h-11 w-11 place-items-center rounded-lg text-text-secondary transition-colors duration-150 hover:bg-warning/10 hover:text-warning dark:text-text-secondary-dark dark:hover:bg-warning/20"
          >
            <Trash2 className="h-4 w-4" aria-hidden />
          </button>
        </div>
      </div>

      {confirmingDelete ? (
        <div className="flex flex-wrap items-center gap-2 rounded-lg border border-warning/40 bg-warning/5 px-3 py-2 dark:bg-warning/10">
          <span className="flex-1 text-body-sm text-text-primary dark:text-text-primary-dark">
            Delete this item?
          </span>
          <Button variant="secondary" onClick={() => setConfirmingDelete(false)}>
            Cancel
          </Button>
          <Button
            className="bg-warning text-white hover:bg-warning/90"
            onClick={handleDelete}
            loading={del.isPending}
          >
            Delete
          </Button>
        </div>
      ) : (
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-caption font-medium uppercase tracking-wide text-text-secondary dark:text-text-secondary-dark">
            Consume
          </span>
          {QUICK_DECREMENTS.map((amount) => (
            <Button
              key={amount}
              variant="secondary"
              disabled={item.quantity < amount || consume.isPending}
              onClick={() => handleConsume(amount)}
              aria-label={`Consume ${amount} ${item.unit}`}
              className="min-w-0 px-3"
            >
              −{formatQuantity(amount)}
            </Button>
          ))}
          {consumeError && (
            <p role="alert" className="w-full text-body-sm text-warning">
              {consumeError}
            </p>
          )}
        </div>
      )}
    </li>
  );
}

function formatQuantity(value: number): string {
  const rounded = Math.round(value * 1000) / 1000;
  return Number.isInteger(rounded) ? rounded.toString() : rounded.toString();
}

interface ExpiryStatus {
  tone: "warning" | "normal";
  label: string;
}

function getExpiryStatus(expiryDate: string | null): ExpiryStatus | null {
  if (!expiryDate) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const expiry = new Date(`${expiryDate}T00:00:00`);
  const days = Math.round((expiry.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  if (days < 0) {
    const abs = Math.abs(days);
    return { tone: "warning", label: `Expired ${abs} day${abs === 1 ? "" : "s"} ago` };
  }
  if (days === 0) return { tone: "warning", label: "Expires today" };
  if (days <= EXPIRING_SOON_DAYS) {
    return { tone: "warning", label: `Expires in ${days} day${days === 1 ? "" : "s"}` };
  }
  return { tone: "normal", label: `Expires ${formatDate(expiry)}` };
}

function formatDate(date: Date): string {
  return date.toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
}
