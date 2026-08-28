import { useMemo, useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { Clock, FileText, Tag, Tags } from "lucide-react";
import { Button } from "./Button";
import { TagInput } from "./TagInput";
import { TextField } from "./TextField";
import { useDistinctRecipeTags } from "../hooks/useRecipes";
import type { RecipeResponse } from "../types/recipe";

export interface RecipeFormValues {
  title: string;
  instructions: string;
  cookTimeMinutes: number | null;
  tags: string[];
}

interface RecipeFormProps {
  mode: "create" | "edit";
  initial?: RecipeResponse;
  submitting?: boolean;
  submitError?: string | null;
  onSubmit: (values: RecipeFormValues) => void | Promise<void>;
  onCancel: () => void;
}

interface FieldErrors {
  title?: string;
  instructions?: string;
  cookTime?: string;
}

const MAX_TITLE = 200;

const TAG_SEEDS = [
  "dinner",
  "breakfast",
  "lunch",
  "quick",
  "vegetarian",
  "dessert",
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

function splitMinutes(total: number | null | undefined): { h: string; m: string } {
  if (total == null) return { h: "", m: "" };
  const h = Math.floor(total / 60);
  const m = total % 60;
  return { h: h ? String(h) : "", m: m ? String(m) : total === 0 ? "0" : "" };
}

export function RecipeForm({
  mode,
  initial,
  submitting = false,
  submitError,
  onSubmit,
  onCancel,
}: RecipeFormProps): ReactNode {
  const initialSplit = splitMinutes(initial?.cookTimeMinutes ?? null);
  const [title, setTitle] = useState(initial?.title ?? "");
  const [instructions, setInstructions] = useState(initial?.instructions ?? "");
  const [hours, setHours] = useState(initialSplit.h);
  const [minutes, setMinutes] = useState(initialSplit.m);
  const [tags, setTags] = useState<string[]>(initial?.tags ?? []);
  const [errors, setErrors] = useState<FieldErrors>({});

  const tagsQuery = useDistinctRecipeTags();
  const tagOptions = useMemo(
    () => mergeSuggestions(TAG_SEEDS, tagsQuery.data ?? []),
    [tagsQuery.data],
  );

  function validate(): { errors: FieldErrors; cookTimeMinutes: number | null } {
    const next: FieldErrors = {};
    if (!title.trim()) next.title = "Title is required.";
    else if (title.length > MAX_TITLE) next.title = `Title must be at most ${MAX_TITLE} characters.`;

    if (!instructions.trim()) next.instructions = "Instructions are required.";

    let cookTimeMinutes: number | null = null;
    const hRaw = hours.trim();
    const mRaw = minutes.trim();
    if (hRaw || mRaw) {
      const h = hRaw ? Number(hRaw) : 0;
      const m = mRaw ? Number(mRaw) : 0;
      if (!Number.isInteger(h) || h < 0 || !Number.isInteger(m) || m < 0) {
        next.cookTime = "Enter non-negative whole numbers.";
      } else if (m > 59) {
        next.cookTime = "Minutes must be 0–59.";
      } else {
        cookTimeMinutes = h * 60 + m;
      }
    }

    return { errors: next, cookTimeMinutes };
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    const { errors: next, cookTimeMinutes } = validate();
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    await onSubmit({
      title: title.trim(),
      instructions: instructions.trim(),
      cookTimeMinutes,
      tags,
    });
  }

  const cookTimeBorder = errors.cookTime
    ? "border-warning focus:border-warning focus:ring-warning"
    : "border-border-subtle focus:border-primary focus:ring-primary dark:border-border-subtle-dark dark:focus:border-primary";

  const cookTimeIconTone = errors.cookTime
    ? "text-warning"
    : "text-text-secondary dark:text-text-secondary-dark";

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      <TextField
        label="Title"
        icon={Tag}
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        error={errors.title}
        placeholder="e.g. Weeknight tomato pasta"
        required
      />
      <div className="flex flex-col gap-1.5">
        <label
          htmlFor="recipe-instructions"
          className="text-body-sm font-medium text-text-primary dark:text-text-primary-dark"
        >
          Instructions
        </label>
        <div className="relative">
          <FileText
            className={
              "pointer-events-none absolute left-3 top-3 h-4 w-4 " +
              (errors.instructions
                ? "text-warning"
                : "text-text-secondary dark:text-text-secondary-dark")
            }
            aria-hidden
          />
          <textarea
            id="recipe-instructions"
            value={instructions}
            onChange={(e) => setInstructions(e.target.value)}
            rows={6}
            aria-invalid={errors.instructions ? true : undefined}
            aria-describedby={errors.instructions ? "recipe-instructions-error" : undefined}
            placeholder="Step-by-step, one per line"
            required
            className={
              "min-h-32 w-full rounded-lg border bg-white py-2 pl-9 pr-3 text-body text-text-primary " +
              "placeholder:text-text-secondary/70 " +
              "focus:outline-none focus:ring-2 focus:ring-offset-0 " +
              "transition-colors duration-150 " +
              "dark:bg-surface-elevated-dark dark:text-text-primary-dark " +
              "dark:placeholder:text-text-secondary-dark/70 " +
              (errors.instructions
                ? "border-warning focus:border-warning focus:ring-warning"
                : "border-border-subtle focus:border-primary focus:ring-primary dark:border-border-subtle-dark dark:focus:border-primary")
            }
          />
        </div>
        {errors.instructions && (
          <p id="recipe-instructions-error" className="text-body-sm text-warning">
            {errors.instructions}
          </p>
        )}
      </div>
      <fieldset className="flex flex-col gap-1.5">
        <legend className="text-body-sm font-medium text-text-primary dark:text-text-primary-dark">
          Cook time
        </legend>
        <div
          className={
            "relative flex min-h-11 items-stretch overflow-hidden rounded-lg border bg-white " +
            "focus-within:outline-none focus-within:ring-2 focus-within:ring-offset-0 " +
            "transition-colors duration-150 dark:bg-surface-elevated-dark " +
            cookTimeBorder
          }
        >
          <Clock
            className={"pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 " + cookTimeIconTone}
            aria-hidden
          />
          <input
            type="number"
            inputMode="numeric"
            step="1"
            min="0"
            aria-label="Hours"
            value={hours}
            onChange={(e) => setHours(e.target.value)}
            placeholder="0"
            data-testid="cook-time-hours"
            className="w-full min-w-0 bg-transparent pl-9 pr-2 text-body text-text-primary placeholder:text-text-secondary/70 focus:outline-none dark:text-text-primary-dark dark:placeholder:text-text-secondary-dark/70"
          />
          <span className="grid place-items-center px-2 text-body-sm text-text-secondary dark:text-text-secondary-dark">
            h
          </span>
          <span className="w-px self-stretch bg-border-subtle dark:bg-border-subtle-dark" aria-hidden />
          <input
            type="number"
            inputMode="numeric"
            step="1"
            min="0"
            max="59"
            aria-label="Minutes"
            value={minutes}
            onChange={(e) => setMinutes(e.target.value)}
            placeholder="0"
            data-testid="cook-time-minutes"
            className="w-full min-w-0 bg-transparent px-2 text-body text-text-primary placeholder:text-text-secondary/70 focus:outline-none dark:text-text-primary-dark dark:placeholder:text-text-secondary-dark/70"
          />
          <span className="grid place-items-center pl-2 pr-3 text-body-sm text-text-secondary dark:text-text-secondary-dark">
            min
          </span>
        </div>
        {errors.cookTime ? (
          <p className="text-body-sm text-warning">{errors.cookTime}</p>
        ) : (
          <p className="text-body-sm text-text-secondary dark:text-text-secondary-dark">Optional</p>
        )}
      </fieldset>
      <TagInput
        label="Tags"
        icon={Tags}
        value={tags}
        onChange={setTags}
        options={tagOptions}
        hint="Optional. Type or pick, press Enter to add."
        placeholder="e.g. dinner, quick"
      />

      {submitError && (
        <div
          role="alert"
          className="rounded-lg border border-warning/40 bg-warning/5 px-3 py-2 text-body-sm text-warning dark:bg-warning/10"
        >
          {submitError}
        </div>
      )}

      <div className="sticky bottom-0 -mx-4 mt-2 flex flex-col-reverse gap-2 border-t border-border-subtle bg-surface-page/95 px-4 py-3 backdrop-blur md:-mx-6 md:flex-row md:justify-end md:px-6 dark:border-border-subtle-dark dark:bg-surface-page-dark/95">
        <Button variant="secondary" type="button" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" loading={submitting}>
          {mode === "create" ? "Create recipe" : "Save changes"}
        </Button>
      </div>
    </form>
  );
}
