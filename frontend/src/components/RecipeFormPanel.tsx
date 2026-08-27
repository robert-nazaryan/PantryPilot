import { useState } from "react";
import type { ReactNode } from "react";
import { RecipeForm } from "./RecipeForm";
import type { RecipeFormValues } from "./RecipeForm";
import { ApiError } from "../api/client";
import {
  useCreateRecipeMutation,
  useUpdateRecipeMutation,
} from "../hooks/useRecipes";
import type { RecipeResponse } from "../types/recipe";

type RecipeFormPanelProps =
  | {
      mode: "create";
      initial?: undefined;
      onCreated: (recipe: RecipeResponse) => void;
      onCancel: () => void;
    }
  | {
      mode: "edit";
      initial: RecipeResponse;
      onSaved: (recipe: RecipeResponse) => void;
      onCancel: () => void;
    };

export function RecipeFormPanel(props: RecipeFormPanelProps): ReactNode {
  const create = useCreateRecipeMutation();
  const update = useUpdateRecipeMutation();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const submitting = props.mode === "create" ? create.isPending : update.isPending;

  async function handleSubmit(values: RecipeFormValues): Promise<void> {
    setSubmitError(null);
    const payload = {
      title: values.title,
      instructions: values.instructions,
      cookTimeMinutes: values.cookTimeMinutes,
      tags: values.tags.length > 0 ? values.tags : null,
    };
    try {
      if (props.mode === "create") {
        const created = await create.mutateAsync(payload);
        props.onCreated(created);
      } else {
        const updated = await update.mutateAsync({ id: props.initial.id, body: payload });
        props.onSaved(updated);
      }
    } catch (err) {
      setSubmitError(
        err instanceof ApiError ? err.message : "Something went wrong. Please try again.",
      );
    }
  }

  return (
    <RecipeForm
      mode={props.mode}
      initial={props.mode === "edit" ? props.initial : undefined}
      submitting={submitting}
      submitError={submitError}
      onSubmit={handleSubmit}
      onCancel={props.onCancel}
    />
  );
}
