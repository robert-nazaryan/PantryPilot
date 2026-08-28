import { useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { Plus, Tag } from "lucide-react";
import { Button } from "./Button";
import { Modal } from "./Modal";
import { TextField } from "./TextField";
import { ApiError } from "../api/client";
import { useCreateShoppingListMutation } from "../hooks/useShoppingLists";

const MAX_NAME = 100;

interface CreateShoppingListModalProps {
  open: boolean;
  onClose: () => void;
  onCreated: (id: number) => void;
}

export function CreateShoppingListModal({
  open,
  onClose,
  onCreated,
}: CreateShoppingListModalProps): ReactNode {
  const create = useCreateShoppingListMutation();
  const [name, setName] = useState("");
  const [nameError, setNameError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);

  function reset(): void {
    setName("");
    setNameError(null);
    setSubmitError(null);
  }

  function handleClose(): void {
    reset();
    onClose();
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    setNameError(null);
    setSubmitError(null);
    if (name.length > MAX_NAME) {
      setNameError(`Name must be at most ${MAX_NAME} characters.`);
      return;
    }
    try {
      const trimmed = name.trim();
      const created = await create.mutateAsync({
        name: trimmed.length > 0 ? trimmed : null,
      });
      reset();
      onCreated(created.id);
    } catch (err) {
      setSubmitError(
        err instanceof ApiError ? err.message : "Couldn't create. Try again.",
      );
    }
  }

  if (!open) return null;

  return (
    <Modal open={open} onClose={handleClose} title="New shopping list">
      <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
        <TextField
          label="Name"
          icon={Tag}
          value={name}
          onChange={(event) => setName(event.target.value)}
          error={nameError}
          hint={nameError ? undefined : "Leave blank to use “Shopping List”."}
          placeholder="e.g. Weekend groceries"
          maxLength={MAX_NAME}
          autoFocus
          data-testid="shopping-list-name-input"
        />
        {submitError && (
          <p role="alert" className="text-body-sm text-warning">
            {submitError}
          </p>
        )}
        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button variant="secondary" type="button" onClick={handleClose}>
            Cancel
          </Button>
          <Button
            type="submit"
            loading={create.isPending}
            data-testid="submit-shopping-list"
          >
            <Plus className="h-4 w-4" aria-hidden />
            Create list
          </Button>
        </div>
      </form>
    </Modal>
  );
}
