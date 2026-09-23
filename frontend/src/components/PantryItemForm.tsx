import { useMemo, useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { CalendarClock, Layers, Scale, Tag } from "lucide-react";
import { Button } from "./Button";
import { Combobox } from "./Combobox";
import { TextField } from "./TextField";
import { useDistinctPantryCategories } from "../hooks/usePantryItems";
import { formatInitialQuantityInput, parseQuantityInput } from "./quantityParse";
import type { QuantityParseResult } from "./quantityParse";
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
  category?: string;
  expiryDate?: string;
}

const MAX_NAME = 200;
const MAX_UNIT = 30;
const MAX_CATEGORY = 50;

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
  const [quantityInput, setQuantityInput] = useState(
    initial ? formatInitialQuantityInput(initial.quantity, initial.unit) : "",
  );
  const [category, setCategory] = useState(initial?.category ?? "");
  const [expiryDate, setExpiryDate] = useState(initial?.expiryDate ?? "");
  const [errors, setErrors] = useState<FieldErrors>({});

  const categoriesQuery = useDistinctPantryCategories();

  const categoryOptions = useMemo(
    () => mergeSuggestions(CATEGORY_SEEDS, categoriesQuery.data ?? []),
    [categoriesQuery.data],
  );

  const parsed = useMemo<QuantityParseResult>(
    () => parseQuantityInput(quantityInput),
    [quantityInput],
  );

  function quantityError(): string | undefined {
    if (parsed.kind === "empty") return "Quantity is required.";
    if (parsed.kind === "unparseable") {
      return "Enter a quantity like \"2 kg\" or \"3\".";
    }
    if (parsed.unit.length > MAX_UNIT) {
      return `Unit must be at most ${MAX_UNIT} characters.`;
    }
    if (mode === "create" && parsed.quantity <= 0) {
      return "Quantity must be greater than 0.";
    }
    if (parsed.quantity < 0) return "Quantity cannot be negative.";
    return undefined;
  }

  function validate(): FieldErrors {
    const next: FieldErrors = {};
    if (!name.trim()) next.name = "Name is required.";
    else if (name.length > MAX_NAME) next.name = `Name must be at most ${MAX_NAME} characters.`;

    const qErr = quantityError();
    if (qErr) next.quantity = qErr;

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
    if (parsed.kind !== "parsed") return;
    await onSubmit({
      name: name.trim(),
      quantity: parsed.quantity,
      unit: parsed.unit,
      category: category.trim() ? category.trim() : null,
      expiryDate: expiryDate ? expiryDate : null,
    });
  }

  const previewText = quantityPreviewText(parsed);
  const previewIsWarning = parsed.kind === "unparseable";

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
      <div className="flex flex-col gap-1">
        <TextField
          label="Quantity"
          icon={Scale}
          value={quantityInput}
          onChange={(e) => setQuantityInput(e.target.value)}
          error={errors.quantity}
          placeholder='e.g. 2 kg, 500 ml, 3'
          inputMode="decimal"
          autoComplete="off"
          required
          data-testid="quantity-input"
        />
        {!errors.quantity && previewText ? (
          <p
            data-testid="quantity-preview"
            className={
              "text-body-sm " +
              (previewIsWarning
                ? "text-warning"
                : "text-text-secondary dark:text-text-secondary-dark")
            }
          >
            {previewText}
          </p>
        ) : null}
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

function quantityPreviewText(parsed: QuantityParseResult): string | null {
  if (parsed.kind === "empty") return null;
  if (parsed.kind === "unparseable") {
    return 'Add a number too, e.g. "2 kg" or "3".';
  }
  const base = `→ ${parsed.quantity} ${parsed.unit}`;
  return parsed.unitAssumed ? `${base} (unit assumed)` : base;
}
