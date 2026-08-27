import { useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { Button } from "./Button";
import { TagInput } from "./TagInput";
import { TextField } from "./TextField";
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
  cookTimeMinutes?: string;
}

const MAX_TITLE = 200;

export function RecipeForm({
  mode,
  initial,
  submitting = false,
  submitError,
  onSubmit,
  onCancel,
}: RecipeFormProps): ReactNode {
  const [title, setTitle] = useState(initial?.title ?? "");
  const [instructions, setInstructions] = useState(initial?.instructions ?? "");
  const [cookTime, setCookTime] = useState(
    initial?.cookTimeMinutes != null ? String(initial.cookTimeMinutes) : "",
  );
  const [tags, setTags] = useState<string[]>(initial?.tags ?? []);
  const [errors, setErrors] = useState<FieldErrors>({});

  function validate(): FieldErrors {
    const next: FieldErrors = {};
    if (!title.trim()) next.title = "Title is required.";
    else if (title.length > MAX_TITLE) next.title = `Title must be at most ${MAX_TITLE} characters.`;

    if (!instructions.trim()) next.instructions = "Instructions are required.";

    if (cookTime.trim()) {
      const parsed = Number(cookTime);
      if (!Number.isInteger(parsed) || parsed < 0) {
        next.cookTimeMinutes = "Cook time must be a non-negative whole number.";
      }
    }

    return next;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    const next = validate();
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    await onSubmit({
      title: title.trim(),
      instructions: instructions.trim(),
      cookTimeMinutes: cookTime.trim() ? Number(cookTime) : null,
      tags,
    });
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      <TextField
        label="Title"
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
            "min-h-32 w-full rounded-lg border bg-white px-3 py-2 text-body text-text-primary " +
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
        {errors.instructions && (
          <p id="recipe-instructions-error" className="text-body-sm text-warning">
            {errors.instructions}
          </p>
        )}
      </div>
      <TextField
        label="Cook time (minutes)"
        type="number"
        inputMode="numeric"
        step="1"
        min="0"
        value={cookTime}
        onChange={(e) => setCookTime(e.target.value)}
        error={errors.cookTimeMinutes}
        placeholder="e.g. 30"
        hint={errors.cookTimeMinutes ? undefined : "Optional"}
      />
      <TagInput
        label="Tags"
        value={tags}
        onChange={setTags}
        hint="Optional. Press Enter or comma to add."
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
