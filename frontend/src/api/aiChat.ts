import { apiFetch } from "./client";
import type { ChatRequest, ChatResponse } from "../types/aiChat";
import type { PantryItemResponse } from "../types/pantry";

export function sendChatMessage(body: ChatRequest): Promise<ChatResponse> {
  return apiFetch<ChatResponse>("/api/ai/chat", { method: "POST", body });
}

export function confirmChatAction(actionId: number): Promise<PantryItemResponse> {
  return apiFetch<PantryItemResponse>(
    `/api/ai/chat/actions/${actionId}/confirm`,
    { method: "POST" },
  );
}
