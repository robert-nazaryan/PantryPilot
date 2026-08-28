import { useMemo, useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { CalendarClock, Layers, Package, Scale, Tag } from "lucide-react";
import { Button } from "./Button";
import { Combobox } from "./Combobox";
import { TextField } from "./TextField";
import {
  useDistinctPantryCategories,
  useDistinctPantryUnits,
} from "../hooks/usePantryItems";
import type { PantryItemResponse } from "../types/pantry";

export interface PantryItemFormValues {
  name: string;
  quantity: number;
  unit: string;
  category: string | null;
  expiryDate: string | null;
}

interface PantryItemFormProps {
  mode: "create" | "edit";
  initial?: PantryItemResponse;
  submitting?: boolean;
  submitError?: string | null;
  onSubmit: (values: PantryItemFormValues) => void | Promise<void>;
  onCancel: () => void;
}

interface FieldErrors {
  name?: string;
  quantity?: string;
  unit?: string;
  category?: string;
  expiryDate?: string;
}

const MAX_NAME = 200;
const MAX_UNIT = 30;
const MAX_CATEGORY = 50;

const UNIT_SEEDS = ["pcs", "g", "kg", "ml", "l", "tsp", "tbsp", "cup", "oz", "lb"];
const CATEGORY_SEEDS = [
  "dairy",
  "produce",
  "meat",
  "grains",
  "spices",
  "frozen",
  "bakery",
  "other",
];

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

export function PantryItemForm({
  mode,
  initial,
  submitting = false,
  submitError,
  onSubmit,
  onCancel,
}: PantryItemFormProps): ReactNode {
  const [name, setName] = useState(initial?.name ?? "");
  const [quantity, setQuantity] = useState(
    initial ? String(initial.quantity) : "",
  );
  const [unit, setUnit] = useState(initial?.unit ?? "");
  const [category, setCategory] = useState(initial?.category ?? "");
  const [expiryDate, setExpiryDate] = useState(initial?.expiryDate ?? "");
  const [errors, setErrors] = useState<FieldErrors>({});

  const unitsQuery = useDistinctPantryUnits();
  const categoriesQuery = useDistinctPantryCategories();

  const unitOptions = useMemo(
    () => mergeSuggestions(UNIT_SEEDS, unitsQuery.data ?? []),
    [unitsQuery.data],
  );
  const categoryOptions = useMemo(
    () => mergeSuggestions(CATEGORY_SEEDS, categoriesQuery.data ?? []),
    [categoriesQuery.data],
  );

  function validate(): FieldErrors {
    const next: FieldErrors = {};
    if (!name.trim()) next.name = "Name is required.";
    else if (name.length > MAX_NAME) next.name = `Name must be at most ${MAX_NAME} characters.`;

    const qty = Number(quantity);
    if (!quantity.trim()) next.quantity = "Quantity is required.";
    else if (Number.isNaN(qty)) next.quantity = "Enter a valid number.";
    else if (mode === "create" && qty <= 0) next.quantity = "Quantity must be greater than 0.";
    else if (qty < 0) next.quantity = "Quantity cannot be negative.";

    if (!unit.trim()) next.unit = "Unit is required.";
    else if (unit.length > MAX_UNIT) next.unit = `Unit must be at most ${MAX_UNIT} characters.`;

    if (category.length > MAX_CATEGORY) {
      next.category = `Category must be at most ${MAX_CATEGORY} characters.`;
    }

    if (mode === "create" && expiryDate) {
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const chosen = new Date(`${expiryDate}T00:00:00`);
      if (chosen.getTime() < today.getTime()) {
        next.expiryDate = "Expiry date can't be in the past.";
      }
    }

    return next;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const next = validate();
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    await onSubmit({
      name: name.trim(),
      quantity: Number(quantity),
      unit: unit.trim(),
      category: category.trim() ? category.trim() : null,
      expiryDate: expiryDate ? expiryDate : null,
    });
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      <TextField
        label="Name"
        icon={Tag}
        value={name}
        onChange={(e) => setName(e.target.value)}
        error={errors.name}
        placeholder="e.g. Whole milk"
        required
      />
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <TextField
          label="Quantity"
          icon={Scale}
          type="number"
          inputMode="decimal"
          step="0.001"
          min="0"
          value={quantity}
          onChange={(e) => setQuantity(e.target.value)}
          error={errors.quantity}
          required
        />
        <Combobox
          label="Unit"
          icon={Package}
          value={unit}
          onChange={setUnit}
          options={unitOptions}
          placeholder="e.g. L, kg, cans"
          error={errors.unit}
          required
          maxLength={MAX_UNIT}
          data-testid="unit-combobox"
        />
      </div>
      <Combobox
        label="Category"
        icon={Layers}
        value={category}
        onChange={setCategory}
        options={categoryOptions}
        placeholder="e.g. Dairy"
        error={errors.category}
        hint={errors.category ? undefined : "Optional"}
        maxLength={MAX_CATEGORY}
        data-testid="category-combobox"
      />
      <TextField
        label="Expiry date"
        icon={CalendarClock}
        type="date"
        value={expiryDate}
        onChange={(e) => setExpiryDate(e.target.value)}
        error={errors.expiryDate}
        hint={errors.expiryDate ? undefined : "Optional"}
      />

      {submitError && (
        <div
          role="alert"
          className="rounded-lg border border-warning/40 bg-warning/5 px-3 py-2 text-body-sm text-warning dark:bg-warning/10"
        >
          {submitError}
        </div>
      )}

      <div className="mt-2 flex flex-col-reverse gap-2 md:flex-row md:justify-end">
        <Button variant="secondary" type="button" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" loading={submitting}>
          {mode === "create" ? "Add item" : "Save changes"}
        </Button>
      </div>
    </form>
  );
}
