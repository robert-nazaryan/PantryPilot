import { useState } from "react";
import type { ReactNode } from "react";
import { PantryItemForm } from "./PantryItemForm";
import type { PantryItemFormValues } from "./PantryItemForm";
import { ApiError } from "../api/client";
import {
  useCreatePantryItemMutation,
  useUpdatePantryItemMutation,
} from "../hooks/usePantryItems";
import type { PantryItemResponse } from "../types/pantry";

type PantryItemFormPanelProps =
  | { mode: "create"; initial?: undefined; onSuccess: () => void; onCancel: () => void }
  | { mode: "edit"; initial: PantryItemResponse; onSuccess: () => void; onCancel: () => void };

export function PantryItemFormPanel(props: PantryItemFormPanelProps): ReactNode {
  const create = useCreatePantryItemMutation();
  const update = useUpdatePantryItemMutation();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const submitting = props.mode === "create" ? create.isPending : update.isPending;

  async function handleSubmit(values: PantryItemFormValues) {
    setSubmitError(null);
    try {
      if (props.mode === "create") {
        await create.mutateAsync(values);
      } else {
        await update.mutateAsync({ id: props.initial.id, body: values });
      }
      props.onSuccess();
    } catch (err) {
      if (err instanceof ApiError) {
        setSubmitError(err.message);
      } else {
        setSubmitError("Something went wrong. Please try again.");
      }
    }
  }

  return (
    <PantryItemForm
      mode={props.mode}
      initial={props.mode === "edit" ? props.initial : undefined}
      submitting={submitting}
      submitError={submitError}
      onSubmit={handleSubmit}
      onCancel={props.onCancel}
    />
  );
}
