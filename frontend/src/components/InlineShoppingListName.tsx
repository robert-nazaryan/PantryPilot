import { useEffect, useRef, useState } from "react";
import type { KeyboardEvent, ReactNode } from "react";
import { Pencil } from "lucide-react";
import { ApiError } from "../api/client";
import { useUpdateShoppingListMutation } from "../hooks/useShoppingLists";

const MAX_NAME = 100;

interface InlineShoppingListNameProps {
  listId: number;
  name: string;
  active: boolean;
}

export function InlineShoppingListName({
  listId,
  name,
  active,
}: InlineShoppingListNameProps): ReactNode {
  const [editing, setEditing] = useState(false);
  const [value, setValue] = useState(name);
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);
  const update = useUpdateShoppingListMutation();

  useEffect(() => {
    if (editing && inputRef.current) {
      inputRef.current.focus();
      inputRef.current.select();
    }
  }, [editing]);

  function startEdit(): void {
    setValue(name);
    setError(null);
    setEditing(true);
  }

  function cancel(): void {
    setValue(name);
    setError(null);
    setEditing(false);
  }

  async function commit(): Promise<void> {
    const trimmed = value.trim();
    if (trimmed.length === 0) {
      setError("Name can't be empty.");
      return;
    }
    if (trimmed.length > MAX_NAME) {
      setError(`Name must be at most ${MAX_NAME} characters.`);
      return;
    }
    if (trimmed === name) {
      setEditing(false);
      setError(null);
      return;
    }
    try {
      await update.mutateAsync({
        id: listId,
        body: { name: trimmed, active },
      });
      setEditing(false);
      setError(null);
    } catch (err) {
      setError(
        err instanceof ApiError ? err.message : "Couldn't rename. Try again.",
      );
    }
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>): void {
    if (event.key === "Enter") {
      event.preventDefault();
      void commit();
    } else if (event.key === "Escape") {
      event.preventDefault();
      cancel();
    }
  }

  if (editing) {
    return (
      <div className="flex flex-col gap-1">
        <input
          ref={inputRef}
          type="text"
          value={value}
          onChange={(event) => setValue(event.target.value)}
          onBlur={() => void commit()}
          onKeyDown={handleKeyDown}
          maxLength={MAX_NAME}
          aria-label="Shopping list name"
          aria-invalid={error ? true : undefined}
          data-testid="shopping-list-name-edit"
          className={
            "min-h-11 w-full rounded-lg border bg-white px-3 text-h1 font-semibold text-text-primary " +
            "focus:outline-none focus:ring-2 focus:ring-primary " +
            "dark:bg-surface-elevated-dark dark:text-text-primary-dark " +
            (error
              ? "border-warning focus:border-warning focus:ring-warning"
              : "border-border-subtle focus:border-primary dark:border-border-subtle-dark")
          }
        />
        {error && (
          <p role="alert" className="text-body-sm text-warning">
            {error}
          </p>
        )}
      </div>
    );
  }

  return (
    <button
      type="button"
      onClick={startEdit}
      className="group inline-flex max-w-full items-center gap-2 rounded-lg text-left focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
      aria-label={`Rename shopping list "${name}"`}
      data-testid="shopping-list-name"
    >
      <span className="min-w-0 truncate text-h1 font-semibold text-text-primary dark:text-text-primary-dark">
        {name}
      </span>
      <Pencil
        className="h-4 w-4 shrink-0 text-text-secondary opacity-0 transition-opacity duration-150 group-hover:opacity-100 group-focus-visible:opacity-100 dark:text-text-secondary-dark"
        aria-hidden
      />
    </button>
  );
}
