import { useMemo, useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { Package, Plus, Scale, Tag } from "lucide-react";
import { Button } from "./Button";
import { Combobox } from "./Combobox";
import { TextField } from "./TextField";
import { ApiError } from "../api/client";
import { useAddIngredientMutation } from "../hooks/useRecipes";
import { useDistinctPantryUnits } from "../hooks/usePantryItems";

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

interface AddIngredientFormProps {
  recipeId: number;
}

const MAX_NAME = 200;
const MAX_UNIT = 30;

export function AddIngredientForm({ recipeId }: AddIngredientFormProps): ReactNode {
  const [open, setOpen] = useState(false);
  const add = useAddIngredientMutation();
  const unitsQuery = useDistinctPantryUnits();
  const unitOptions = useMemo(
    () => mergeSuggestions(UNIT_SEEDS, unitsQuery.data ?? []),
    [unitsQuery.data],
  );

  const [name, setName] = useState("");
  const [quantity, setQuantity] = useState("");
  const [unit, setUnit] = useState("");
  const [nameError, setNameError] = useState<string | null>(null);
  const [quantityError, setQuantityError] = useState<string | null>(null);
  const [unitError, setUnitError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);

  function reset(): void {
    setName("");
    setQuantity("");
    setUnit("");
    setNameError(null);
    setQuantityError(null);
    setUnitError(null);
    setSubmitError(null);
  }

  function handleCancel(): void {
    reset();
    setOpen(false);
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
      await add.mutateAsync({
        recipeId,
        body: {
          name: name.trim(),
          quantity: qty,
          unit: unit.trim() ? unit.trim() : null,
        },
      });
      reset();
      setOpen(false);
    } catch (err) {
      setSubmitError(
        err instanceof ApiError ? err.message : "Couldn't add ingredient. Try again.",
      );
    }
  }

  if (!open) {
    return (
      <Button variant="secondary" onClick={() => setOpen(true)} data-testid="add-ingredient-button">
        <Plus className="h-4 w-4" aria-hidden />
        Add ingredient
      </Button>
    );
  }

  return (
    <div className="rounded-lg border border-border-subtle bg-white p-4 dark:border-border-subtle-dark dark:bg-surface-card-dark">
      <h3 className="text-body font-semibold text-text-primary dark:text-text-primary-dark">
        Add ingredient
      </h3>
      <form onSubmit={handleSubmit} noValidate className="mt-3 flex flex-col gap-3">
        <TextField
          label="Name"
          icon={Tag}
          value={name}
          onChange={(e) => setName(e.target.value)}
          error={nameError}
          placeholder="e.g. Flour"
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
            maxLength={30}
            data-testid="ingredient-unit-combobox"
          />
        </div>
        {submitError && (
          <p role="alert" className="text-body-sm text-warning">
            {submitError}
          </p>
        )}
        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button variant="secondary" type="button" onClick={handleCancel}>
            Cancel
          </Button>
          <Button type="submit" loading={add.isPending} data-testid="submit-ingredient">
            <Plus className="h-4 w-4" aria-hidden />
            Add
          </Button>
        </div>
      </form>
    </div>
  );
}
