import { ApiError } from "../api/client";

export type ChatErrorKind =
  | "flag_off"
  | "network"
  | "rate_limited"
  | "provider_error"
  | "session_expired"
  | "session_missing"
  | "server_error"
  | "unknown";

export type ConfirmErrorKind =
  | "flag_off"
  | "network"
  | "insufficient_quantity"
  | "stale_action"
  | "action_missing"
  | "session_expired"
  | "server_error"
  | "unknown";

export interface ChatErrorDetail {
  kind: ChatErrorKind;
  message: string;
}

export interface ConfirmErrorDetail {
  kind: ConfirmErrorKind;
  message: string;
}

function isNetworkError(err: unknown): boolean {
  return err instanceof TypeError || (err instanceof Error && err.message.includes("Failed to fetch"));
}

export function classifySendError(err: unknown): ChatErrorDetail {
  if (isNetworkError(err)) {
    return {
      kind: "network",
      message: "Can't reach the assistant — you may be offline. Check your connection and try again.",
    };
  }
  if (err instanceof ApiError) {
    if (err.status === 503 || err.code === "ai_unavailable") {
      return {
        kind: "flag_off",
        message: "The assistant is turned off right now — try again later.",
      };
    }
    if (err.status === 429 || err.code === "rate_limited") {
      return {
        kind: "rate_limited",
        message: "The assistant is busy right now. Give it a moment and try again.",
      };
    }
    if (err.status === 502 || err.status === 504) {
      return {
        kind: "provider_error",
        message: "The AI provider timed out. Please send your message again.",
      };
    }
    if (err.status === 500) {
      return {
        kind: "server_error",
        message: "Something on our side broke. Please try again shortly.",
      };
    }
    if (err.status === 404) {
      return {
        kind: "session_missing",
        message: "This chat session no longer exists. Start a new chat and try again.",
      };
    }
    if (err.status === 401) {
      return {
        kind: "session_expired",
        message: "Your session expired. Sign in again to continue chatting.",
      };
    }
  }
  return {
    kind: "unknown",
    message: "Something went wrong reaching the assistant. Please try again.",
  };
}

export function classifyConfirmError(err: unknown): ConfirmErrorDetail {
  if (isNetworkError(err)) {
    return {
      kind: "network",
      message: "Can't reach the server — check your connection and try again.",
    };
  }
  if (err instanceof ApiError) {
    if (err.status === 503 || err.code === "ai_unavailable") {
      return {
        kind: "flag_off",
        message: "The assistant is turned off — this action can't be confirmed right now.",
      };
    }
    if (err.code === "insufficient_quantity") {
      return {
        kind: "insufficient_quantity",
        message: "Not enough on hand to consume that much — ask again with a smaller amount.",
      };
    }
    if (err.code === "stale_chat_action" || err.status === 409) {
      return {
        kind: "stale_action",
        message: "This proposal is no longer valid — ask again to try a fresh one.",
      };
    }
    if (err.status === 404) {
      return {
        kind: "action_missing",
        message: "This proposal is no longer available.",
      };
    }
    if (err.status === 401) {
      return {
        kind: "session_expired",
        message: "Your session expired. Sign in again to confirm.",
      };
    }
    if (err.status >= 500) {
      return {
        kind: "server_error",
        message: "Something on our side broke while applying that. Please try again shortly.",
      };
    }
  }
  return {
    kind: "unknown",
    message: "Could not apply this change. Please try again.",
  };
}

const REL_TIME_UNITS: ReadonlyArray<readonly [string, number]> = [
  ["y", 31_536_000_000],
  ["mo", 2_592_000_000],
  ["d", 86_400_000],
  ["h", 3_600_000],
  ["m", 60_000],
];

/** Concise relative time: "just now", "5m ago", "3h ago", "2d ago". */
export function formatRelativeTime(fromEpochMs: number, nowEpochMs: number): string {
  const delta = Math.max(0, nowEpochMs - fromEpochMs);
  if (delta < 45_000) return "just now";
  for (const [unit, unitMs] of REL_TIME_UNITS) {
    if (delta >= unitMs) {
      const n = Math.floor(delta / unitMs);
      return `${n}${unit} ago`;
    }
  }
  return "just now";
}
