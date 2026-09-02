import { useEffect, useMemo, useRef, useState } from "react";
import type { FormEvent, KeyboardEvent as ReactKeyboardEvent, ReactNode } from "react";
import {
  Calendar,
  Check,
  Hash,
  Layers,
  Loader2,
  Minus,
  Package,
  PenSquare,
  Pencil,
  Send,
  Sparkles,
  Tag,
  Trash2,
  X,
} from "lucide-react";
import { useConfirmChatActionMutation, useSendChatMessageMutation } from "../hooks/useAiChat";
import { ApiError } from "../api/client";
import type {
  ConsumePantryItemPayload,
  CreatePantryItemPayload,
  DeletePantryItemPayload,
  ProposedAction,
  UpdatePantryItemPayload,
} from "../types/aiChat";

const SESSION_STORAGE_KEY = "pantrypilot.ai.sessionId";

interface AiChatPanelProps {
  open: boolean;
  onClose: () => void;
}

type ActionLifecycle = "pending" | "confirming" | "confirmed" | "dismissed" | "failed";

interface ActionEntry {
  kind: "action";
  entryId: string;
  action: ProposedAction;
  status: ActionLifecycle;
  errorMessage?: string;
}

type TranscriptEntry =
  | { kind: "user"; content: string }
  | { kind: "assistant"; content: string }
  | { kind: "error"; content: string }
  | ActionEntry;

function loadPersistedSessionId(): number | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(SESSION_STORAGE_KEY);
  if (!raw) return null;
  const parsed = Number.parseInt(raw, 10);
  return Number.isFinite(parsed) ? parsed : null;
}

function persistSessionId(id: number | null): void {
  if (typeof window === "undefined") return;
  if (id === null) window.localStorage.removeItem(SESSION_STORAGE_KEY);
  else window.localStorage.setItem(SESSION_STORAGE_KEY, String(id));
}

export function AiChatPanel({ open, onClose }: AiChatPanelProps): ReactNode {
  const [sessionId, setSessionId] = useState<number | null>(() => loadPersistedSessionId());
  const [transcript, setTranscript] = useState<TranscriptEntry[]>([]);
  const [draft, setDraft] = useState("");
  const scrollAnchorRef = useRef<HTMLDivElement | null>(null);
  const inputRef = useRef<HTMLTextAreaElement | null>(null);

  const sendMutation = useSendChatMessageMutation();
  const confirmMutation = useConfirmChatActionMutation();

  useEffect(() => {
    if (!open) return;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  useEffect(() => {
    if (open) inputRef.current?.focus();
  }, [open]);

  useEffect(() => {
    scrollAnchorRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [transcript, sendMutation.isPending]);

  function resetSession(): void {
    setSessionId(null);
    persistSessionId(null);
    setTranscript([]);
    setDraft("");
    sendMutation.reset();
    inputRef.current?.focus();
  }

  function updateActionEntry(entryId: string, patch: Partial<ActionEntry>): void {
    setTranscript((prev) =>
      prev.map((e) => (e.kind === "action" && e.entryId === entryId ? { ...e, ...patch } : e)),
    );
  }

  function handleConfirmAction(entry: ActionEntry): void {
    updateActionEntry(entry.entryId, { status: "confirming", errorMessage: undefined });
    confirmMutation.mutate(entry.action.actionId, {
      onSuccess: () => {
        updateActionEntry(entry.entryId, { status: "confirmed" });
      },
      onError: (err) => {
        updateActionEntry(entry.entryId, {
          status: "failed",
          errorMessage: friendlyConfirmError(err),
        });
      },
    });
  }

  function handleDismissAction(entry: ActionEntry): void {
    updateActionEntry(entry.entryId, { status: "dismissed" });
  }

  function submit(): void {
    const trimmed = draft.trim();
    if (!trimmed || sendMutation.isPending) return;
    setTranscript((prev) => [...prev, { kind: "user", content: trimmed }]);
    setDraft("");
    sendMutation.mutate(
      { sessionId, message: trimmed },
      {
        onSuccess: (data) => {
          setSessionId(data.sessionId);
          persistSessionId(data.sessionId);
          setTranscript((prev) => {
            const next: TranscriptEntry[] = [...prev, { kind: "assistant", content: data.reply }];
            if (data.proposedAction) {
              next.push({
                kind: "action",
                entryId: `action-${data.proposedAction.actionId}`,
                action: data.proposedAction,
                status: "pending",
              });
            }
            return next;
          });
        },
        onError: (err) => {
          const msg = friendlySendError(err);
          setTranscript((prev) => [...prev, { kind: "error", content: msg }]);
        },
      },
    );
  }

  function onFormSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault();
    submit();
  }

  function onTextareaKeyDown(event: ReactKeyboardEvent<HTMLTextAreaElement>): void {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      submit();
    }
  }

  const isEmpty = transcript.length === 0 && !sendMutation.isPending;

  if (!open) return null;

  return (
    <>
      <div
        aria-hidden
        className="fixed inset-0 z-40 bg-black/40 md:hidden dark:bg-black/60"
        onClick={onClose}
      />
      <section
        role="dialog"
        aria-modal="true"
        aria-label="PantryPilot assistant"
        id="ai-chat-panel"
        className={
          "fixed z-50 flex flex-col overflow-hidden border border-border-subtle bg-white shadow-lg " +
          "dark:border-border-subtle-dark dark:bg-surface-card-dark " +
          "inset-0 rounded-none " +
          "md:inset-auto md:bottom-24 md:right-6 md:h-[min(600px,80vh)] md:w-[380px] md:rounded-2xl"
        }
      >
        <header className="flex items-center justify-between gap-2 border-b border-border-subtle px-4 py-3 dark:border-border-subtle-dark">
          <div className="flex items-center gap-2">
            <span className="grid h-8 w-8 place-items-center rounded-lg bg-primary/10 text-primary dark:bg-primary/15">
              <Sparkles className="h-4 w-4" aria-hidden />
            </span>
            <div className="flex flex-col leading-tight">
              <span className="text-body font-semibold text-text-primary dark:text-text-primary-dark">
                Assistant
              </span>
              <span className="text-caption text-text-secondary dark:text-text-secondary-dark">
                Grounded in your pantry &amp; recipes
              </span>
            </div>
          </div>
          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={resetSession}
              aria-label="Start a new chat"
              title="Start a new chat"
              className="grid h-9 w-9 place-items-center rounded-lg text-text-secondary transition-colors duration-150 hover:bg-surface-card hover:text-text-primary dark:text-text-secondary-dark dark:hover:bg-surface-elevated-dark dark:hover:text-text-primary-dark"
            >
              <PenSquare className="h-4 w-4" aria-hidden />
            </button>
            <button
              type="button"
              onClick={onClose}
              aria-label="Close assistant"
              className="grid h-9 w-9 place-items-center rounded-lg text-text-secondary transition-colors duration-150 hover:bg-surface-card hover:text-text-primary dark:text-text-secondary-dark dark:hover:bg-surface-elevated-dark dark:hover:text-text-primary-dark"
            >
              <X className="h-5 w-5" aria-hidden />
            </button>
          </div>
        </header>

        <div className="flex-1 overflow-y-auto bg-surface-page px-4 py-4 dark:bg-surface-page-dark">
          {isEmpty ? <EmptyPrompt /> : null}
          <ul className="flex flex-col gap-3">
            {transcript.map((entry, idx) => (
              <TranscriptRow
                key={entry.kind === "action" ? entry.entryId : idx}
                entry={entry}
                onConfirm={handleConfirmAction}
                onDismiss={handleDismissAction}
              />
            ))}
            {sendMutation.isPending && <TypingRow />}
          </ul>
          <div ref={scrollAnchorRef} />
        </div>

        <form
          onSubmit={onFormSubmit}
          className="border-t border-border-subtle bg-white p-3 dark:border-border-subtle-dark dark:bg-surface-card-dark"
        >
          <label htmlFor="ai-chat-input" className="sr-only">
            Message the assistant
          </label>
          <div className="flex items-end gap-2">
            <textarea
              id="ai-chat-input"
              ref={inputRef}
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              onKeyDown={onTextareaKeyDown}
              placeholder="Ask about your pantry, recipes, or shopping…"
              rows={1}
              maxLength={4000}
              className={
                "min-h-11 max-h-40 w-full resize-none rounded-lg border border-border-subtle bg-white px-3 py-2 " +
                "text-body text-text-primary placeholder:text-text-secondary/70 " +
                "focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary " +
                "transition-colors duration-150 " +
                "dark:border-border-subtle-dark dark:bg-surface-elevated-dark dark:text-text-primary-dark " +
                "dark:placeholder:text-text-secondary-dark/70"
              }
            />
            <button
              type="submit"
              disabled={!draft.trim() || sendMutation.isPending}
              aria-label="Send message"
              className={
                "grid h-11 w-11 shrink-0 place-items-center rounded-lg bg-primary text-white " +
                "transition-colors duration-150 hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-60 " +
                "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
              }
            >
              {sendMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" aria-hidden />
              ) : (
                <Send className="h-4 w-4" aria-hidden />
              )}
            </button>
          </div>
        </form>
      </section>
    </>
  );
}

function EmptyPrompt(): ReactNode {
  const suggestions = useMemo(
    () => [
      "What can I cook tonight?",
      "Add 2 liters of milk to my pantry.",
      "Which items are expiring soon?",
    ],
    [],
  );
  return (
    <div className="mb-4 rounded-xl border border-border-subtle bg-white p-4 dark:border-border-subtle-dark dark:bg-surface-card-dark">
      <p className="text-body-sm text-text-secondary dark:text-text-secondary-dark">
        Try asking:
      </p>
      <ul className="mt-2 flex flex-col gap-1">
        {suggestions.map((s) => (
          <li
            key={s}
            className="text-body-sm text-text-primary dark:text-text-primary-dark"
          >
            &ldquo;{s}&rdquo;
          </li>
        ))}
      </ul>
    </div>
  );
}

interface TranscriptRowProps {
  entry: TranscriptEntry;
  onConfirm: (entry: ActionEntry) => void;
  onDismiss: (entry: ActionEntry) => void;
}

function TranscriptRow({ entry, onConfirm, onDismiss }: TranscriptRowProps): ReactNode {
  if (entry.kind === "user") {
    return (
      <li className="flex justify-end">
        <div className="max-w-[85%] rounded-2xl rounded-br-md bg-primary px-3 py-2 text-body-sm text-white shadow-sm">
          {entry.content}
        </div>
      </li>
    );
  }
  if (entry.kind === "error") {
    return (
      <li className="flex justify-start">
        <div className="max-w-[85%] rounded-2xl rounded-bl-md border border-warning/40 bg-warning/10 px-3 py-2 text-body-sm text-warning">
          {entry.content}
        </div>
      </li>
    );
  }
  if (entry.kind === "action") {
    return (
      <li className="flex justify-start">
        <ProposedActionCard entry={entry} onConfirm={onConfirm} onDismiss={onDismiss} />
      </li>
    );
  }
  return (
    <li className="flex justify-start">
      <div className="max-w-[85%] whitespace-pre-wrap rounded-2xl rounded-bl-md border border-border-subtle bg-white px-3 py-2 text-body-sm text-text-primary shadow-sm dark:border-border-subtle-dark dark:bg-surface-card-dark dark:text-text-primary-dark">
        {entry.content}
      </div>
    </li>
  );
}

interface ProposedActionCardProps {
  entry: ActionEntry;
  onConfirm: (entry: ActionEntry) => void;
  onDismiss: (entry: ActionEntry) => void;
}

function ProposedActionCard({ entry, onConfirm, onDismiss }: ProposedActionCardProps): ReactNode {
  const { action, status } = entry;
  const cardBase =
    "w-full max-w-[95%] rounded-2xl rounded-bl-md border p-3 shadow-sm transition-colors duration-150";
  const cardTone =
    status === "confirmed"
      ? "border-success/40 bg-success/10 dark:border-success/40 dark:bg-success/15"
      : status === "dismissed"
        ? "border-border-subtle bg-surface-card opacity-70 dark:border-border-subtle-dark dark:bg-surface-elevated-dark"
        : status === "failed"
          ? "border-warning/40 bg-warning/10"
          : "border-border-subtle bg-white dark:border-border-subtle-dark dark:bg-surface-card-dark";

  const { icon: HeaderIcon, title } = actionHeader(action);

  return (
    <div className={`${cardBase} ${cardTone}`}>
      <div className="mb-2 flex items-center gap-2">
        <span className="grid h-6 w-6 place-items-center rounded-md bg-primary/10 text-primary dark:bg-primary/15">
          <HeaderIcon className="h-3.5 w-3.5" aria-hidden />
        </span>
        <p className="text-body-sm font-medium text-text-primary dark:text-text-primary-dark">
          {title}
        </p>
      </div>
      <ActionCardBody action={action} />
      <ActionCardFooter entry={entry} onConfirm={onConfirm} onDismiss={onDismiss} />
    </div>
  );
}

type IconType = typeof Sparkles;

function actionHeader(action: ProposedAction): { icon: IconType; title: string } {
  switch (action.type) {
    case "CREATE_PANTRY_ITEM":
      return { icon: Sparkles, title: "Add to pantry" };
    case "UPDATE_PANTRY_ITEM":
      return { icon: Pencil, title: "Update pantry item" };
    case "DELETE_PANTRY_ITEM":
      return { icon: Trash2, title: "Remove from pantry" };
    case "CONSUME_PANTRY_ITEM":
      return { icon: Minus, title: "Use from pantry" };
  }
}

function ActionCardBody({ action }: { action: ProposedAction }): ReactNode {
  switch (action.type) {
    case "CREATE_PANTRY_ITEM":
      return <CreateActionBody payload={action.payload} />;
    case "UPDATE_PANTRY_ITEM":
      return <UpdateActionBody payload={action.payload} />;
    case "DELETE_PANTRY_ITEM":
      return <DeleteActionBody payload={action.payload} />;
    case "CONSUME_PANTRY_ITEM":
      return <ConsumeActionBody payload={action.payload} />;
  }
}

function CreateActionBody({ payload }: { payload: CreatePantryItemPayload }): ReactNode {
  return (
    <dl className="mb-3 grid grid-cols-[16px_1fr] gap-x-2 gap-y-1 text-body-sm text-text-primary dark:text-text-primary-dark">
      <Tag className="mt-0.5 h-4 w-4 text-text-secondary dark:text-text-secondary-dark" aria-hidden />
      <span>
        <span className="sr-only">Name: </span>
        {payload.name}
      </span>
      <Hash className="mt-0.5 h-4 w-4 text-text-secondary dark:text-text-secondary-dark" aria-hidden />
      <span>
        <span className="sr-only">Quantity: </span>
        {payload.quantity} {payload.unit}
      </span>
      {payload.category ? (
        <>
          <Layers className="mt-0.5 h-4 w-4 text-text-secondary dark:text-text-secondary-dark" aria-hidden />
          <span>
            <span className="sr-only">Category: </span>
            {payload.category}
          </span>
        </>
      ) : (
        <>
          <Package className="mt-0.5 h-4 w-4 text-text-secondary/50 dark:text-text-secondary-dark/50" aria-hidden />
          <span className="text-text-secondary dark:text-text-secondary-dark">Uncategorized</span>
        </>
      )}
      {payload.expiryDate ? (
        <>
          <Calendar className="mt-0.5 h-4 w-4 text-text-secondary dark:text-text-secondary-dark" aria-hidden />
          <span>
            <span className="sr-only">Expires: </span>
            Expires {payload.expiryDate}
          </span>
        </>
      ) : null}
    </dl>
  );
}

function UpdateActionBody({ payload }: { payload: UpdatePantryItemPayload }): ReactNode {
  return (
    <dl className="mb-3 grid grid-cols-[16px_1fr] gap-x-2 gap-y-1 text-body-sm text-text-primary dark:text-text-primary-dark">
      <Tag className="mt-0.5 h-4 w-4 text-text-secondary dark:text-text-secondary-dark" aria-hidden />
      <span>
        <span className="sr-only">Item: </span>
        {payload.name}
      </span>
      <Hash className="mt-0.5 h-4 w-4 text-text-secondary dark:text-text-secondary-dark" aria-hidden />
      <span>
        <span className="sr-only">New quantity: </span>
        {payload.quantity} {payload.unit}
      </span>
      {payload.category ? (
        <>
          <Layers className="mt-0.5 h-4 w-4 text-text-secondary dark:text-text-secondary-dark" aria-hidden />
          <span>
            <span className="sr-only">Category: </span>
            {payload.category}
          </span>
        </>
      ) : null}
      {payload.expiryDate ? (
        <>
          <Calendar className="mt-0.5 h-4 w-4 text-text-secondary dark:text-text-secondary-dark" aria-hidden />
          <span>
            <span className="sr-only">Expires: </span>
            Expires {payload.expiryDate}
          </span>
        </>
      ) : null}
    </dl>
  );
}

function DeleteActionBody({ payload }: { payload: DeletePantryItemPayload }): ReactNode {
  return (
    <div className="mb-3 flex items-center gap-2 text-body-sm text-text-primary dark:text-text-primary-dark">
      <Trash2 className="h-4 w-4 text-text-secondary dark:text-text-secondary-dark" aria-hidden />
      <span>
        <span className="sr-only">Remove: </span>
        Remove <span className="font-medium">{payload.name}</span> from your pantry
      </span>
    </div>
  );
}

function ConsumeActionBody({ payload }: { payload: ConsumePantryItemPayload }): ReactNode {
  const remaining = Math.max(0, payload.availableQuantity - payload.quantity);
  const overdraw = payload.quantity > payload.availableQuantity;
  return (
    <dl className="mb-3 grid grid-cols-[16px_1fr] gap-x-2 gap-y-1 text-body-sm text-text-primary dark:text-text-primary-dark">
      <Tag className="mt-0.5 h-4 w-4 text-text-secondary dark:text-text-secondary-dark" aria-hidden />
      <span>
        <span className="sr-only">Item: </span>
        {payload.name}
      </span>
      <Minus className="mt-0.5 h-4 w-4 text-text-secondary dark:text-text-secondary-dark" aria-hidden />
      <span>
        Use{" "}
        <span className="font-medium">
          {payload.quantity} {payload.unit}
        </span>
      </span>
      <Hash className="mt-0.5 h-4 w-4 text-text-secondary dark:text-text-secondary-dark" aria-hidden />
      <span className={overdraw ? "text-warning" : undefined}>
        {payload.availableQuantity} {payload.unit} → {remaining} {payload.unit} remaining
        {overdraw ? " (not enough on hand)" : ""}
      </span>
    </dl>
  );
}

interface ActionCardFooterProps {
  entry: ActionEntry;
  onConfirm: (entry: ActionEntry) => void;
  onDismiss: (entry: ActionEntry) => void;
}

function confirmedLabel(type: ProposedAction["type"]): string {
  switch (type) {
    case "CREATE_PANTRY_ITEM":
      return "Added to your pantry";
    case "UPDATE_PANTRY_ITEM":
      return "Updated";
    case "DELETE_PANTRY_ITEM":
      return "Removed from your pantry";
    case "CONSUME_PANTRY_ITEM":
      return "Consumed";
  }
}

function ActionCardFooter({ entry, onConfirm, onDismiss }: ActionCardFooterProps): ReactNode {
  if (entry.status === "confirmed") {
    return (
      <div className="flex items-center gap-2 text-body-sm font-medium text-success">
        <Check className="h-4 w-4" aria-hidden />
        {confirmedLabel(entry.action.type)}
      </div>
    );
  }
  if (entry.status === "dismissed") {
    return (
      <div className="flex items-center gap-2 text-body-sm text-text-secondary dark:text-text-secondary-dark">
        <X className="h-4 w-4" aria-hidden />
        Dismissed
      </div>
    );
  }
  if (entry.status === "failed") {
    return (
      <div className="flex flex-col gap-2">
        <p className="text-body-sm text-warning">
          {entry.errorMessage ?? "Could not add this item."}
        </p>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => onConfirm(entry)}
            className="inline-flex min-h-9 items-center gap-1 rounded-lg bg-primary px-3 text-body-sm font-medium text-white transition-colors duration-150 hover:bg-primary-hover"
          >
            Try again
          </button>
          <button
            type="button"
            onClick={() => onDismiss(entry)}
            className="inline-flex min-h-9 items-center gap-1 rounded-lg border border-border-subtle bg-white px-3 text-body-sm font-medium text-text-primary transition-colors duration-150 hover:bg-surface-card dark:border-border-subtle-dark dark:bg-surface-elevated-dark dark:text-text-primary-dark dark:hover:bg-border-subtle-dark"
          >
            Dismiss
          </button>
        </div>
      </div>
    );
  }
  const isConfirming = entry.status === "confirming";
  return (
    <div className="flex items-center gap-2">
      <button
        type="button"
        onClick={() => onConfirm(entry)}
        disabled={isConfirming}
        className="inline-flex min-h-9 items-center gap-1 rounded-lg bg-primary px-3 text-body-sm font-medium text-white transition-colors duration-150 hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-60"
      >
        {isConfirming ? (
          <Loader2 className="h-4 w-4 animate-spin" aria-hidden />
        ) : (
          <Check className="h-4 w-4" aria-hidden />
        )}
        Confirm
      </button>
      <button
        type="button"
        onClick={() => onDismiss(entry)}
        disabled={isConfirming}
        className="inline-flex min-h-9 items-center gap-1 rounded-lg border border-border-subtle bg-white px-3 text-body-sm font-medium text-text-primary transition-colors duration-150 hover:bg-surface-card disabled:cursor-not-allowed disabled:opacity-60 dark:border-border-subtle-dark dark:bg-surface-elevated-dark dark:text-text-primary-dark dark:hover:bg-border-subtle-dark"
      >
        Cancel
      </button>
    </div>
  );
}

function TypingRow(): ReactNode {
  return (
    <li className="flex justify-start" aria-live="polite" aria-label="Assistant is typing">
      <div className="flex items-center gap-1 rounded-2xl rounded-bl-md border border-border-subtle bg-white px-3 py-3 shadow-sm dark:border-border-subtle-dark dark:bg-surface-card-dark">
        <span className="h-2 w-2 animate-bounce rounded-full bg-text-secondary/60 [animation-delay:-0.3s] dark:bg-text-secondary-dark/60" />
        <span className="h-2 w-2 animate-bounce rounded-full bg-text-secondary/60 [animation-delay:-0.15s] dark:bg-text-secondary-dark/60" />
        <span className="h-2 w-2 animate-bounce rounded-full bg-text-secondary/60 dark:bg-text-secondary-dark/60" />
      </div>
    </li>
  );
}

function friendlySendError(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.status === 503 || err.code === "ai_unavailable") {
      return "The assistant isn't available right now — check back later.";
    }
    if (err.status === 404) {
      return "This chat session no longer exists. Start a new chat and try again.";
    }
    if (err.status === 401) {
      return "Your session expired. Sign in again to continue chatting.";
    }
  }
  return "Something went wrong reaching the assistant. Please try again.";
}

function friendlyConfirmError(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.code === "insufficient_quantity") {
      return "Not enough on hand to consume that much — ask again with a smaller amount.";
    }
    if (err.code === "stale_chat_action" || err.status === 409) {
      return "This proposal is no longer valid — ask again to try a fresh one.";
    }
    if (err.status === 404) {
      return "This proposal is no longer available.";
    }
    if (err.status === 401) {
      return "Your session expired. Sign in again to confirm.";
    }
  }
  return "Could not apply this change. Please try again.";
}
