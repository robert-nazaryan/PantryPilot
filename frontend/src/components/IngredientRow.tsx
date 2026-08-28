import { useMemo, useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { Check, Package, Pencil, Scale, Tag, Trash2, X } from "lucide-react";
import { Button } from "./Button";
import { Combobox } from "./Combobox";
import { TextField } from "./TextField";
import { ApiError } from "../api/client";
import {
  useDeleteIngredientMutation,
  useUpdateIngredientMutation,
} from "../hooks/useRecipes";
import { useDistinctPantryUnits } from "../hooks/usePantryItems";
import type { RecipeIngredientResponse } from "../types/recipe";

const UNIT_SEEDS = ["pcs", "g", "kg", "ml", "l", "tsp", "tbsp", "cup", "oz", "lb"];

function mergeSuggestions(seeds: string[], userValues: string[]): string[] {
  const seen = new Set<string>();
  const out: string[] = [];
  for (const v of [...userValues, ...seeds]) {
    const key = v.trim().toLowerCase();
    if (!key || seen.has(key)) continue;
    seen.add(key);
    out.push(v.trim());
  }
  return out;
}

interface IngredientRowProps {
  recipeId: number;
  ingredient: RecipeIngredientResponse;
}

const MAX_NAME = 200;
const MAX_UNIT = 30;

export function IngredientRow({ recipeId, ingredient }: IngredientRowProps): ReactNode {
  const [editing, setEditing] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  const update = useUpdateIngredientMutation();
  const del = useDeleteIngredientMutation();
  const unitsQuery = useDistinctPantryUnits();
  const unitOptions = useMemo(
    () => mergeSuggestions(UNIT_SEEDS, unitsQuery.data ?? []),
    [unitsQuery.data],
  );

  const [name, setName] = useState(ingredient.name);
  const [quantity, setQuantity] = useState(String(ingredient.quantity));
  const [unit, setUnit] = useState(ingredient.unit ?? "");
  const [nameError, setNameError] = useState<string | null>(null);
  const [quantityError, setQuantityError] = useState<string | null>(null);
  const [unitError, setUnitError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);

  function startEdit(): void {
    setName(ingredient.name);
    setQuantity(String(ingredient.quantity));
    setUnit(ingredient.unit ?? "");
    setNameError(null);
    setQuantityError(null);
    setUnitError(null);
    setSubmitError(null);
    setEditing(true);
  }

  function cancelEdit(): void {
    setEditing(false);
    setSubmitError(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    let ok = true;
    setNameError(null);
    setQuantityError(null);
    setUnitError(null);
    setSubmitError(null);

    if (!name.trim()) {
      setNameError("Name is required.");
      ok = false;
    } else if (name.length > MAX_NAME) {
      setNameError(`Name must be at most ${MAX_NAME} characters.`);
      ok = false;
    }
    const qty = Number(quantity);
    if (!quantity.trim() || Number.isNaN(qty)) {
      setQuantityError("Enter a valid number.");
      ok = false;
    } else if (qty <= 0) {
      setQuantityError("Quantity must be greater than 0.");
      ok = false;
    }
    if (unit.length > MAX_UNIT) {
      setUnitError(`Unit must be at most ${MAX_UNIT} characters.`);
      ok = false;
    }
    if (!ok) return;

    try {
      await update.mutateAsync({
        recipeId,
        ingredientId: ingredient.id,
        body: {
          name: name.trim(),
          quantity: qty,
          unit: unit.trim() ? unit.trim() : null,
        },
      });
      setEditing(false);
    } catch (err) {
      setSubmitError(
        err instanceof ApiError ? err.message : "Couldn't save. Try again.",
      );
    }
  }

  async function handleDelete(): Promise<void> {
    setSubmitError(null);
    try {
      await del.mutateAsync({ recipeId, ingredientId: ingredient.id });
    } catch (err) {
      setSubmitError(
        err instanceof ApiError ? err.message : "Couldn't delete. Try again.",
      );
      setConfirmingDelete(false);
    }
  }

  if (editing) {
    return (
      <li
        data-testid={`ingredient-row-${ingredient.id}`}
        className="rounded-lg border border-border-subtle bg-white p-3 dark:border-border-subtle-dark dark:bg-surface-card-dark"
      >
        <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-3">
          <TextField
            label="Name"
            icon={Tag}
            value={name}
            onChange={(e) => setName(e.target.value)}
            error={nameError}
            required
          />
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <TextField
              label="Quantity"
              icon={Scale}
              type="number"
              inputMode="decimal"
              step="0.001"
              min="0"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              error={quantityError}
              required
            />
            <Combobox
              label="Unit"
              icon={Package}
              value={unit}
              onChange={setUnit}
              options={unitOptions}
              error={unitError}
              placeholder="e.g. g, cups"
              hint={unitError ? undefined : "Optional"}
              maxLength={MAX_UNIT}
            />
          </div>
          {submitError && (
            <p role="alert" className="text-body-sm text-warning">
              {submitError}
            </p>
          )}
          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <Button variant="secondary" type="button" onClick={cancelEdit}>
              Cancel
            </Button>
            <Button type="submit" loading={update.isPending}>
              <Check className="h-4 w-4" aria-hidden />
              Save
            </Button>
          </div>
        </form>
      </li>
    );
  }

  return (
    <li
      data-testid={`ingredient-row-${ingredient.id}`}
      className="flex flex-col gap-2 rounded-lg border border-border-subtle bg-white p-3 md:p-4 dark:border-border-subtle-dark dark:bg-surface-card-dark"
    >
      <div className="flex items-center justify-between gap-3">
        <div className="min-w-0 flex-1">
          <p className="text-body font-medium text-text-primary dark:text-text-primary-dark">
            {ingredient.name}
          </p>
          <p className="mt-0.5 text-body-sm text-text-secondary dark:text-text-secondary-dark">
            {formatQuantity(ingredient.quantity)}
            {ingredient.unit ? ` ${ingredient.unit}` : ""}
          </p>
        </div>
        <div className="flex shrink-0 items-center gap-1">
          <button
            type="button"
            aria-label={`Edit ${ingredient.name}`}
            onClick={startEdit}
            className="grid h-11 w-11 place-items-center rounded-lg text-text-secondary transition-colors duration-150 hover:bg-surface-card hover:text-text-primary dark:text-text-secondary-dark dark:hover:bg-surface-elevated-dark dark:hover:text-text-primary-dark"
          >
            <Pencil className="h-4 w-4" aria-hidden />
          </button>
          <button
            type="button"
            aria-label={`Delete ${ingredient.name}`}
            onClick={() => setConfirmingDelete(true)}
            className="grid h-11 w-11 place-items-center rounded-lg text-text-secondary transition-colors duration-150 hover:bg-warning/10 hover:text-warning dark:text-text-secondary-dark dark:hover:bg-warning/20"
          >
            <Trash2 className="h-4 w-4" aria-hidden />
          </button>
        </div>
      </div>
      {confirmingDelete && (
        <div className="flex flex-wrap items-center gap-2 rounded-lg border border-warning/40 bg-warning/5 px-3 py-2 dark:bg-warning/10">
          <span className="flex-1 text-body-sm text-text-primary dark:text-text-primary-dark">
            Remove this ingredient?
          </span>
          <Button variant="secondary" onClick={() => setConfirmingDelete(false)}>
            <X className="h-4 w-4" aria-hidden />
            Cancel
          </Button>
          <Button
            className="bg-warning text-white hover:bg-warning/90"
            onClick={handleDelete}
            loading={del.isPending}
          >
            <Trash2 className="h-4 w-4" aria-hidden />
            Remove
          </Button>
        </div>
      )}
      {submitError && !confirmingDelete && (
        <p role="alert" className="text-body-sm text-warning">
          {submitError}
        </p>
      )}
    </li>
  );
}

function formatQuantity(value: number): string {
  const rounded = Math.round(value * 1000) / 1000;
  return rounded.toString();
}
