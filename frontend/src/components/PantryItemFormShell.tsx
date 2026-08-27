import { useEffect } from "react";
import type { ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { Modal } from "./Modal";
import { PantryItemFormPanel } from "./PantryItemFormPanel";
import { MD_BREAKPOINT_QUERY, useMediaQuery } from "../hooks/useMediaQuery";
import type { PantryItemResponse } from "../types/pantry";

type PantryItemFormShellProps =
  | {
      open: boolean;
      mode: "create";
      initial?: undefined;
      onClose: () => void;
      onSuccess: () => void;
    }
  | {
      open: boolean;
      mode: "edit";
      initial: PantryItemResponse;
      onClose: () => void;
      onSuccess: () => void;
    };

export function PantryItemFormShell(props: PantryItemFormShellProps): ReactNode {
  const isDesktop = useMediaQuery(MD_BREAKPOINT_QUERY);
  const navigate = useNavigate();

  useEffect(() => {
    if (!props.open || isDesktop) return;
    const target =
      props.mode === "create" ? "/pantry/new" : `/pantry/${props.initial.id}/edit`;
    navigate(target);
    props.onClose();
  }, [props, isDesktop, navigate]);

  if (!isDesktop || !props.open) return null;

  const title = props.mode === "create" ? "Add pantry item" : "Edit pantry item";
  return (
    <Modal open={props.open} onClose={props.onClose} title={title}>
      {props.mode === "create" ? (
        <PantryItemFormPanel
          mode="create"
          onSuccess={props.onSuccess}
          onCancel={props.onClose}
        />
      ) : (
        <PantryItemFormPanel
          mode="edit"
          initial={props.initial}
          onSuccess={props.onSuccess}
          onCancel={props.onClose}
        />
      )}
    </Modal>
  );
}
