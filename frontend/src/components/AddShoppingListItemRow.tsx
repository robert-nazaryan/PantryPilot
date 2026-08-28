import { useMemo, useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { Package, Plus, Scale, Tag } from "lucide-react";
import { Button } from "./Button";
import { Combobox } from "./Combobox";
import { TextField } from "./TextField";
import { ApiError } from "../api/client";
import { useAddShoppingListItemMutation } from "../hooks/useShoppingLists";
import { useDistinctPantryUnits } from "../hooks/usePantryItems";

const UNIT_SEEDS = ["pcs", "g", "kg", "ml", "l", "tsp", "tbsp", "cup", "oz", "lb"];
const MAX_NAME = 200;
const MAX_UNIT = 30;

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

interface AddShoppingListItemRowProps {
  listId: number;
}

export function AddShoppingListItemRow({
  listId,
}: AddShoppingListItemRowProps): ReactNode {
  const add = useAddShoppingListItemMutation();
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
        listId,
        body: {
          name: name.trim(),
          quantity: qty,
          unit: unit.trim() ? unit.trim() : null,
        },
      });
      reset();
    } catch (err) {
      setSubmitError(
        err instanceof ApiError ? err.message : "Couldn't add item. Try again.",
      );
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      noValidate
      className="flex flex-col gap-3 rounded-lg border border-dashed border-border-subtle bg-white p-3 md:p-4 dark:border-border-subtle-dark dark:bg-surface-card-dark"
      data-testid="add-shopping-list-item-form"
    >
      <TextField
        label="Item"
        icon={Tag}
        value={name}
        onChange={(event) => setName(event.target.value)}
        error={nameError}
        placeholder="Add an item"
        maxLength={MAX_NAME}
        data-testid="add-shopping-list-item-name"
      />
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-[1fr_1fr_auto] sm:items-end">
        <TextField
          label="Quantity"
          icon={Scale}
          type="number"
          inputMode="decimal"
          step="0.001"
          min="0"
          value={quantity}
          onChange={(event) => setQuantity(event.target.value)}
          error={quantityError}
          data-testid="add-shopping-list-item-quantity"
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
          data-testid="add-shopping-list-item-unit"
        />
        <Button
          type="submit"
          loading={add.isPending}
          data-testid="submit-shopping-list-item"
        >
          <Plus className="h-4 w-4" aria-hidden />
          Add
        </Button>
      </div>
      {submitError && (
        <p role="alert" className="text-body-sm text-warning">
          {submitError}
        </p>
      )}
    </form>
  );
}
