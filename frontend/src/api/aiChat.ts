import { apiFetch } from "./client";
import type { ChatRequest, ChatResponse, ConfirmActionResponse } from "../types/aiChat";

export function sendChatMessage(body: ChatRequest): Promise<ChatResponse> {
  return apiFetch<ChatResponse>("/api/ai/chat", { method: "POST", body });
}

export function confirmChatAction(actionId: number): Promise<ConfirmActionResponse> {
  return apiFetch<ConfirmActionResponse>(
    `/api/ai/chat/actions/${actionId}/confirm`,
    { method: "POST" },
  );
}
